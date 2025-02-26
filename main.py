from openai import OpenAI
import requests
import json
import time
import asyncio
from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
from pathlib import Path
import os
import uuid
from fastapi.middleware.cors import CORSMiddleware

# ✅ FastAPI 초기화
app = FastAPI()

# ✅ CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ✅ 업로드 폴더 설정
UPLOAD_DIR = Path("uploads") if os.name == "nt" else Path("/tmp/uploads")
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)




# ✅ OpenAI API 클라이언트 초기화
client = OpenAI(api_key="<your_OpenAI_API_key>")  # 🔹 OpenAI API 키 입력

# ✅ Clova OCR API 설정
CLOVA_API_URL = "<your_API_URL>"
CLOVA_SECRET_KEY = "<your_SECRET_KEY>"

# ✅ Clova OCR 호출 함수
def clova_ocr(image_path):
    headers = {"X-OCR-SECRET": CLOVA_SECRET_KEY}
    payload = {
        "version": "V2",
        "requestId": str(uuid.uuid4()),
        "timestamp": int(time.time() * 1000),
        "images": [{"format": image_path.suffix.replace(".", ""), "name": "ocr_image"}]
    }

    with open(image_path, "rb") as f:
        files = {"file": f}
        response = requests.post(CLOVA_API_URL, headers=headers, data={"message": json.dumps(payload)}, files=files)

    if response.status_code == 200:
        return response.json()
    else:
        return {"error": f"Request failed with status code {response.status_code}", "details": response.text}

# ✅ OCR에서 텍스트와 좌표 추출
def extract_text_and_positions(ocr_result):
    extracted_data = []
    
    if "images" in ocr_result:
        for image in ocr_result["images"]:
            if "fields" in image:
                for field in image["fields"]:
                    text = field["inferText"]
                    position = field["boundingPoly"]["vertices"][0]  # 좌측 상단 (첫 글자 좌표)
                    extracted_data.append({"text": text, "x": position["x"], "y": position["y"]})

    return extracted_data

# ✅ GPT를 활용하여 OCR 결과를 JSON으로 정제
def gpt_refine_text(extracted_data):
    prompt = f"""
    다음은 OCR을 통해 추출한 텍스트 데이터입니다.  
    모든 유형의 문서를 분석하여 JSON 형식으로 정리하세요.

    ✅ **중요 규칙 (필수 준수!)**
    - 1. OCR로 감지된 모든 텍스트를 유지하세요. (누락 금지!)
    - 2. 잘못된 공백이나 단어가 합쳐진 경우 자동 수정하세요.
    - 예: `"짜장 소스"` → `"짜장소스"` (✅ 올바른 병합)  
    - 예: `"김치 만두"` → `"김치만두"` (✅ 올바른 병합)  
    - 3. 각 항목에 좌표(x, y)를 포함하세요. (절대 누락 금지!)
    - 4. 문서 유형을 자동 감지하고 올바른 구조로 정리하세요.
    - 5. 메뉴판이라면 가격을 정확히 매칭하고, 설명서라면 부품 목록을 구분하세요.
    - 6. 원문과 비교하는 방식으로 요청하여 의미가 변경될 경우 수정하지 마세요.
    - 7. 문장의 의미가 없거나 오타가 존재 시 6번을 무시하고 임의로 변경 하세요.
    

    **📌 문서 유형 자동 분류**
    1️⃣ **메뉴판 (`menu`)**
       - **메뉴명과 가격을 원본 그대로 유지하세요.**
       - **카테고리가 있을 경우 유지, 없으면 제외하세요.**
       - **추가 정보(포장 가능 여부, 옵션 등)는 리스트로 따로 정리하세요.**
    
    2️⃣ **설명서 (`manual`)**
       - **설명 문장과 부품 목록을 따로 구분하세요.**
    
    3️⃣ **안내문 (`notice`)**
       - **문장을 리스트로 정리하세요.**

    OCR 결과:
    {json.dumps(extracted_data, ensure_ascii=False)}

    ✅ **출력 형식 (JSON)**
    {{
      "document_type": "menu / manual / notice",
      "sections": [
        {{
          "type": "table",
          "headers": ["메뉴", "가격"],
          "rows": [
            {{
              "data": ["메뉴명", "가격"],
              "coordinates": {{
                "x": x좌표,
                "y": y좌표
              }}
            }}
          ]
        }},
        {{
          "type": "list",
          "items": [
            {{
              "name": "추가 정보 문구",
              "coordinates": {{
                "x": x좌표,
                "y": y좌표
              }}
            }}
          ]
        }},
        {{
          "type": "text",
          "content": "설명 문장",
          "coordinates": {{
            "x": x좌표,
            "y": y좌표
          }}
        }}
      ]
    }}
    """

    response = client.chat.completions.create(
        model="gpt-4o",
        messages=[{"role": "user", "content": prompt}],
        response_format={"type": "json_object"}
    )

    return json.loads(response.choices[0].message.content)

# ✅ OpenAI GPT 요청을 비동기 처리
async def gpt_refine_text_async(extracted_data):
    return await asyncio.to_thread(gpt_refine_text, extracted_data)

# ✅ FastAPI 엔드포인트
@app.post("/upload_and_refine/")
async def upload_and_refine(file: UploadFile = File(...)):
    unique_filename = f"{uuid.uuid4().hex}{Path(file.filename).suffix.lower()}"
    file_path = UPLOAD_DIR / unique_filename

    with file_path.open("wb") as buffer:
        buffer.write(await file.read())

    ocr_result = clova_ocr(file_path)
    extracted_data = extract_text_and_positions(ocr_result)

    if not extracted_data:
        return JSONResponse(content={"error": "No text found in the image.", "ocr_result": ocr_result})

    # ✅ OCR 결과 정제 및 JSON 변환
    refined_text = await gpt_refine_text_async(extracted_data)

    return JSONResponse(content={
        "filename": unique_filename,
        "original_filename": file.filename,
        "refined_text": refined_text,
        "message": "File uploaded, OCR processed, and text refined successfully"
    })

# ✅ FastAPI 실행
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
