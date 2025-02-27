import os
import json
import openai
import asyncio
import httpx
from fastapi import FastAPI, Request, File, UploadFile, Form
from fastapi.middleware.cors import CORSMiddleware
from firebase_admin import initialize_app, storage  # firebase_functions는 삭제
from pydantic import BaseModel
from fastapi.responses import ORJSONResponse
import logging
import base64
import uuid
from starlette.responses import JSONResponse
import requests
from dotenv import load_dotenv
from google.cloud import storage

logging.basicConfig(level=logging.DEBUG, format="%(asctime)s - %(levelname)s - %(message)s")

# --------------------------------------------------
# 1. Firebase 초기화 (firebase_admin)
# --------------------------------------------------
#  - Cloud Run에서 Firebase(Storage 등) 사용 시, Service Account 인증 필요
#  - 예: GOOGLE_APPLICATION_CREDENTIALS 환경 변수를 설정하여 JSON Key 사용
import firebase_admin
from firebase_admin import credentials

cred = credentials.Certificate('service_account.json')
firebase_admin.initialize_app(cred,{
    "storageBucket": "dbdb-96e12.firebasestorage.app"
})

# --------------------------------------------------
# 2. FastAPI 앱 생성
# --------------------------------------------------
app = FastAPI()

load_dotenv()

# --------------------------------------------------
# 3. 환경 변수에서 OPENAI_API_KEY 가져오기
# --------------------------------------------------
API_KEY = os.getenv("OPENAI_API_KEY")
if not API_KEY:
    raise ValueError("🚨 환경 변수 `OPENAI_API_KEY`가 설정되지 않았습니다! `.env` 파일을 확인하세요.")

API_URL ="https://api.openai.com/v1/chat/completions"

# --------------------------------------------------
# 4. CORS 설정
# --------------------------------------------------
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --------------------------------------------------
# 5. Pydantic 모델 정의
# --------------------------------------------------
class AgentModel(BaseModel):
    """사용자의 입력 텍스트를 저장하는 모델"""
    user_text: str

class RecordModel(BaseModel):
    """텍스트 데이터를 업로드할 때 사용하는 모델"""
    result_text: str
    sourceType:str
    languageType: str

class ImageModel(BaseModel):
    file: UploadFile = File(...)
    input:str
    result:str

# --------------------------------------------------
# 6. GPT(챗봇) 관련 로직
# --------------------------------------------------
latest_response = {}  # 최근 GPT 응답 메모리 저장용

@app.post("/agent/")
async def upload_agent(agent: AgentModel):
    """
    예: { "user_text": "이주 노동자 고민..." }
    """
    global latest_response
    user_input = agent.user_text
    logging.info(f"📥 user_text 값: {user_input}")

    # 동기 함수 chat_with_gpt를 쓰레드 풀에서 실행
    latest_response = await asyncio.to_thread(chat_with_gpt, user_input)
    logging.info(f"🤖 GPT 응답: {latest_response}")
    return {"status": "success", "response": latest_response}

def chat_with_gpt(prompt: str, model: str = "gpt-4o"):
    """OpenAI ChatGPT API 호출"""
    headers = {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json"
    }

    # 3가지 해결책을 요청하는 프롬프트 예시
    modified_prompt = (
        f"사용자가 다음과 같은 고민을 말했습니다:\n\n"
        f"\"{prompt}\"\n\n"
        f"이 문제를 해결할 수 있는 3가지 방법을 설명해줘. "
        f"각 방법을 '1.', '2.', '3.'으로 시작하도록 해줘."
        f"{prompt}언어를 감지한 후, 동일한 언어로 답변해줘."

)

    data = {
        "model": model,
        "messages": [{"role": "user", "content": modified_prompt}],
        "temperature": 0.7
    }

    try:
        response = requests.post(API_URL, headers=headers, json=data)
        response.raise_for_status()
        full_response = response.json().get("choices", [{}])[0].get("message", {}).get("content", "")

        # GPT 응답 파싱
        solutions = [s.strip() for s in full_response.split("\n") if s.strip()]
        response_data = {"response_1": "", "response_2": "", "response_3": ""}
        solution_count = 0

        for line in solutions:
            if line.startswith("1.") and solution_count == 0:
                response_data["response_1"] = line
                solution_count += 1
            elif line.startswith("2.") and solution_count == 1:
                response_data["response_2"] = line
                solution_count += 1
            elif line.startswith("3.") and solution_count == 2:
                response_data["response_3"] = line
                solution_count += 1

        return response_data

    except requests.exceptions.RequestException as e:
        return {"error": f"API 요청 중 오류 발생: {e}"}

@app.get("/get_agent_text")
async def get_response():
    """저장된 GPT 응답을 반환"""
    logging.info(f"📌 latest_response 상태: {latest_response}")
    if latest_response.get("response_1"):
        return {
            "status": "success",
            "text1": latest_response["response_1"],
            "text2": latest_response["response_2"],
            "text3": latest_response["response_3"]
        }
    else:
        return {
            "status": "fail",
            "text": "❌ No response available!"
        }

# --------------------------------------------------
# 7. Firebase Storage + 이미지 업로드 + OCR 서버
# --------------------------------------------------

import asyncio
import httpx

@app.get("/translated_results")
async def get_translated_results():
    """
    GET 엔드포인트: 저장된 번역 결과(번역된 텍스트 및 좌표값)를 안드로이드에서 조회할 수 있도록 반환
    """
    return JSONResponse(content={
        "translated_text_data": {
            "text": text_data,
            "table": table_data,
            "list": list_data
        }
    })

list_data = []
table_data = []
text_data = []

@app.post("/upload_image/")
async def upload_image( file: UploadFile = File(...),input:str=Form(...),result:str=Form(...)):
    """
    1) 이미지를 Firebase Storage에 업로드
    2) OCR 서버에 이미지 URL을 전달하여 refined_text 획득
    3) refined_text의 각 섹션에 대해 번역 서버 호출하여 번역 결과 포함
    4) 전역 변수 (list_data, table_data, text_data)에 각 데이터 타입별로 저장
    """
    global list_data, table_data, text_data

    list_data = []
    table_data = []
    text_data = []

    try:
        logging.info("✅ 이미지 업로드 요청 수신")
        max_file_size = 10 * 1024 * 1024  # 10MB
        file_content = await file.read()
        file_size = len(file_content)

        if file_size > max_file_size:
            return JSONResponse(
                content={"error": "파일 크기가 10MB를 초과했습니다."},
                status_code=400
            )

        filename = "image.jpg"
        file_location = f"/tmp/{filename}"
        with open(file_location, "wb") as f:
            f.write(file_content)

        logging.info(f"📏 파일 크기: {file_size} bytes")
        logging.info(f"📄 파일 이름: {file.filename}")
        logging.info(f"🖼️ 파일 유형: {file.content_type}")

        # Firebase Storage 업로드
        client = storage.Client()
        firebase_bucket_name = "dbdb-96e12.firebasestorage.app"  # 본인 버킷명
        bucket = client.bucket(firebase_bucket_name)
        blob = bucket.blob(filename)
        blob.upload_from_filename(file_location)
        blob.make_public()

        image_url = blob.public_url
        logging.info(f"📸 업로드 성공: {image_url}")

        # OCR 서버에 전달 (예시 URL)
        ocr_url = "http://172.30.16.141:8000/upload_and_refine/"
        files = {"file": open(file_location, "rb")}
        ocr_response = requests.post(ocr_url, files=files)
        ocr_result = ocr_response.json()
        logging.info(f"📥 OCR 서버 응답: {ocr_response.status_code} - {ocr_response.text}")

        # refined_text 및 섹션 추출
        refined_text = ocr_result.get("refined_text", {})
        sections = refined_text.get("sections", [])

        # 기본 번역 설정
        source_language = input          # OCR 결과의 원본 언어
        target_language = result      # 번역할 대상 언어
    

        # 번역 요청은 비동기로 처리
        async with httpx.AsyncClient() as client_async:
            for section in sections:
                section_type = section.get("type")

                # 번역 없이도 데이터 저장할 수도 있지만, 번역을 원한다면 아래와 같이 처리합니다.
                if section_type == "table":
                    # 테이블: headers와 각 셀(cell)의 텍스트 번역
                    headers = section.get("headers", [])
                    translated_headers = []
                    for header in headers:
                        payload = {
                            "text": header,
                            "source_language": source_language,
                            "target_languages": [target_language]
                        }
                        translate_url = f"https://4af3-211-171-154-226.ngrok-free.app/translate/?text={header}&source_language={source_language}&target_languages={target_language}"
                        # translate_url ="https://3fa7-211-171-154-226.ngrok-free.app/translate/?text={header}&source_language={input_type}&target_languages={language_type}"
                        resp = await client_async.post(translate_url, json=payload)
                        if resp.status_code == 200:
                            translated_header = next(iter(resp.json().get("translated_texts", {}).values()), "Translation error")
                        else:
                            translated_header = "Translation error"
                        translated_headers.append(translated_header)

                    rows = section.get("rows", [])
                    translated_rows = []
                    for row in rows:
                        # row는 예를 들어 {"data": ["메뉴명", "가격"], "coordinates": {...}} 형태라고 가정
                        row_data = row.get("data", [])
                        translated_row_data = []
                        for cell in row_data:
                            payload = {
                                "text": cell,
                                "source_language": source_language,
                                "target_languages": [target_language]
                            }
                            translate_url=f"https://4af3-211-171-154-226.ngrok-free.app/translate/?text={cell}&source_language={source_language}&target_languages={target_language}"
                            resp = await client_async.post(translate_url, json=payload)
                            if resp.status_code == 200:
                                translated_cell = next(iter(resp.json().get("translated_texts", {}).values()), "Translation error")
                            else:
                                translated_cell = "Translation error"
                            translated_row_data.append(translated_cell)
                        translated_rows.append({
                            "data": translated_row_data,
                            "coordinates": row.get("coordinates", {})
                        })

                    table_data.append({
                        "headers": translated_headers,
                        "rows": translated_rows
                    })

                elif section_type == "list":
                    # 리스트: 각 항목의 'name' 필드 번역
                    items = section.get("items", [])
                    for item in items:
                        original_name = item.get("name", "")
                        payload = {
                            "text": original_name,
                            "source_language": source_language,
                            "target_languages": [target_language]
                        }
                        translate_url=f"https://4af3-211-171-154-226.ngrok-free.app/translate/?text={original_name}&source_language={source_language}&target_languages={target_language}"
                        resp = await client_async.post(translate_url, json=payload)
                        if resp.status_code == 200:
                            translated_name = next(iter(resp.json().get("translated_texts", {}).values()), "Translation error")
                        else:
                            translated_name = "Translation error"
                        list_data.append({
                            "name": original_name,
                            "translated": translated_name,
                            "x": item.get("coordinates", {}).get("x", 0),
                            "y": item.get("coordinates", {}).get("y", 0)
                        })

                elif section_type == "text":
                    # 텍스트: content 필드 번역
                    original_text = section.get("content", "")
                    payload = {
                        "text": original_text,
                        "source_language": source_language,
                        "target_languages": [target_language]
                    }
                    resp = await client_async.post(translate_url, json=payload)
                    if resp.status_code == 200:
                        translated_texts = resp.json().get("translated_texts", {})
                        translated_text = next(iter(translated_texts.values()), "Translation error")
                    else:
                        translated_text = "Translation error"
                    
                    text_data.append({
                        "content": original_text,
                        "translated": translated_text,
                        "x": section.get("coordinates", {}).get("x", 0),
                        "y": section.get("coordinates", {}).get("y", 0)
                    })

        # 디버그 출력
        print("text=" + str(text_data))
        print("table=" + str(table_data))
        print("list=" + str(list_data))


        return {
            "filename": filename,
            "image_url": image_url,
            "ocr_result": ocr_result,
            "translated_text_data": {
                "text": text_data,
                "table": table_data,
                "list": list_data
            },
            "message": "Image uploaded, OCR processed, and text translated successfully"
        }

    except Exception as e:
        logging.error(f"❌ Image upload failed: {str(e)}")
        return JSONResponse(content={"error": str(e)}, status_code=500)
    



# --------------------------------------------------
# 8. 텍스트 번역 (번역 서버 연동)
# --------------------------------------------------
translated_text_store = {}

@app.post("/upload_text/")
async def upload_text(record: RecordModel):
    """
    JSON 예시:
    {
      "result_text": "번역할 내용",
      "languageType": "인도네시아어"
    }
    """
    try:
        result = record.result_text
        language_type = record.languageType
        input_type=record.sourceType
        
        logging.info(f"📨 Received text: {result} (Language: {language_type})")

        # 번역 서버 URL - 예: ngrok 주소
        # translate_url = "https://e839-211-171-154-226.ngrok-free.app/translate/?text={result}&source_language=한국어&target_languages={language_type}"
        translate_url="https://4af3-211-171-154-226.ngrok-free.app/translate/?text={result}&source_language={input_type}&target_languages={language_type}"
        payload = {
            "text": result,
            "source_language":input_type,
            "target_languages": [language_type]
        }
        async with httpx.AsyncClient() as client:
            response = await client.post(translate_url, json=payload)

        if response.status_code == 200:
            translated_texts = response.json().get("translated_texts", {})
            # 딕셔너리에서 첫 번째 번역 결과만 추출
            translated_text = next(iter(translated_texts.values()), "Translation error")
            translated_text_store["latest"] = translated_text
            logging.info(f"📄 Translation result: {translated_texts}")
        else:
            translated_texts = {"error": "Translation error"}
            logging.error(f"❌ Translation failed (status_code={response.status_code})")

        return {
            "original_text": result,
            "translated_texts": translated_texts,
            "languageType": language_type,
            "message": "Text received and translated successfully"
        }

    except Exception as e:
        logging.error(f"번역 처리 중 오류 발생: {str(e)}")
        return JSONResponse(content={"error": str(e)}, status_code=500)

@app.get("/get_translated_text")
async def get_translated_text():
    """저장된 번역 결과 반환"""
    if "latest" in translated_text_store:
        return {"message": translated_text_store["latest"]}
    else:
        return {"message": "No translated text available"}

# --------------------------------------------------
# 9. 기본 루트 엔드포인트
# --------------------------------------------------
@app.get("/")
def read_root():
    return {"message": "Welcome to the FastAPI app running on Cloud Run!"}

#===============================================
#추가함

@app.get("/check_key/")
def check_key():
    key = os.getenv("OPENAI_API_KEY")
    if key:
        return {"status": "success", "key": key[:5] + "****(masked)"}
    else:
        return {"status": "fail", "reason": "API 키가 없습니다."}


# --------------------------------------------------
# 10. 로컬/Cloud Run 실행 설정
# --------------------------------------------------
if __name__ == "__main__":
    port = int(os.getenv("PORT", 8080))
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=port,http="h11")
