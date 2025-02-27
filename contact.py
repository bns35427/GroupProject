from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from transformers import AutoModelForSeq2SeqLM, AutoTokenizer
import torch
from typing import List
import os
import openai
# from langdetect import detect

app = FastAPI()

# 환경 변수에서 OpenAI API 키 가져오기
api_key = os.getenv("OPENAI_API_KEY")
if not api_key:
    raise ValueError("환경변수 'OPENAI_API_KEY'가 설정되지 않았습니다. API 키를 확인하세요.")
openai.api_key = api_key

# 모델 경로
model_path = "C:/Users/USER/Desktop/MainProj/nllb_finetuned_model_test"

# 경로 확인
if not os.path.exists(model_path):
    raise FileNotFoundError(f"모델 경로가 존재하지 않습니다: {model_path}")
print(f"모델 경로: {model_path}")
print(f"디렉토리 내용: {os.listdir(model_path)}")

# 모델과 토크나이저 로드
try:
    tokenizer = AutoTokenizer.from_pretrained(
        model_path,
        use_fast=False,
        local_files_only=True
    )
    model = AutoModelForSeq2SeqLM.from_pretrained(
        model_path,
        device_map="auto",
        local_files_only=True
    )
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model.to(device)
    model.eval()
    print("모델과 토크나이저 로드 성공")
except Exception as e:
    raise RuntimeError(f"모델 또는 토크나이저 로드에 실패했습니다: {str(e)}") from e

# 지원 언어 목록 (NLLB 코드 기준)
LANGUAGES = {
    "한국어": "kor_Hang",
    "영어": "eng_Latn",
    "크메르어": "khm_Khmr",
    "버마어": "mya_Mymr",
    "인도네시아어": "ind_Latn",
    "네팔어": "npi_Deva",
    "베트남어": "vie_Latn"
}

# 로컬 NLLB 모델로 번역 수행 함수
def translate_nllb(text: str, source_lang_code: str, target_lang_codes: List[str]) -> dict:
    if not text.strip():
        raise ValueError("입력 문장이 비어 있습니다.")

    print(f"🔹 NLLB 번역 요청: '{text}' ({source_lang_code})")

    # 입력 문장 앞에 소스 언어 코드 추가
    formatted_input = f"<<{source_lang_code}>> {text}"

    # 입력 문장 토큰화
    inputs = tokenizer(
        formatted_input,
        return_tensors="pt",
        max_length=128,
        truncation=True,
        padding="longest"
    )
    inputs = {key: val.to(device) for key, val in inputs.items()}

    # 각 대상 언어에 대해 번역 수행
    results = {}
    for target_lang_code in target_lang_codes:
        with torch.no_grad():
            output_tokens = model.generate(
                **inputs,
                max_length=128,
                forced_bos_token_id=tokenizer.lang_code_to_id[target_lang_code],
                repetition_penalty=2.0,
                num_beams=5
            )
        translated_text = tokenizer.decode(output_tokens[0], skip_special_tokens=True).strip().replace('"', '')
        results[target_lang_code] = translated_text
        print(f"✅ NLLB 번역 완료 ({target_lang_code}): {translated_text}")

    return results

# OpenAI로 맥락에 맞게 재번역하는 함수
def translate_openai(original_text: str, nllb_result: str, target_language: str) -> str:
    prompt = (
        f"The following sentence's original text and NLLB translation are provided. "
        f"Refine the translation naturally in {target_language} and respond only in {target_language}. "
        f"Do not use any other language.\n\n"
        f"Original: {original_text}\n"
        f"NLLB Translation: {nllb_result}\n"
        f"Target Language: {target_language}"
    )
    
    response = openai.ChatCompletion.create(
        model="gpt-3.5-turbo",
        messages=[
            {"role": "system", "content": f"You are a highly skilled translator refining translations. Always respond in {target_language} only."},
            {"role": "user", "content": prompt}
        ],
        max_tokens=500,
        temperature=0.7
    )
    
    refined_text = response.choices[0].message["content"].strip()
    print(f"✅ OpenAI 재번역 완료 ({target_language}): {refined_text}")
    return refined_text

# 요청 데이터 모델
class TranslationRequest(BaseModel):
    text: str
    source_language: str
    target_languages: List[str]

@app.post("/translate/")
async def translate_text(request: TranslationRequest):
    try:
        # 소스 언어와 대상 언어가 지원되는 언어인지 확인
        if request.source_language not in LANGUAGES:
            raise HTTPException(
                status_code=400,
                detail=f"지원하지 않는 소스 언어입니다: {request.source_language}"
            )
        
        invalid_languages = [lang for lang in request.target_languages if lang not in LANGUAGES]
        if invalid_languages:
            raise HTTPException(
                status_code=400,
                detail=f"지원하지 않는 대상 언어가 포함되어 있습니다: {', '.join(invalid_languages)}"
            )

        # NLLB로 번역 수행
        nllb_results = translate_nllb(
            request.text,
            LANGUAGES[request.source_language],
            [LANGUAGES[lang] for lang in request.target_languages]
        )
        
        # OpenAI로 재번역 수행
        translated_texts = {}
        for lang in request.target_languages:
            nllb_result = nllb_results[LANGUAGES[lang]]
            refined_text = translate_openai(request.text, nllb_result, lang)
            translated_texts[lang] = refined_text
        
        return {"translated_texts": translated_texts}
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"서버 오류 발생: {str(e)}")

@app.get("/translate/")
async def translate_get(text: str, source_language: str, target_languages: str):
    try:
        languages = [lang.strip() for lang in target_languages.split(",")]

        # 소스 언어와 대상 언어가 지원되는 언어인지 확인
        if source_language not in LANGUAGES:
            raise HTTPException(
                status_code=400,
                detail=f"지원하지 않는 소스 언어입니다: {source_language}"
            )
        
        invalid_languages = [lang for lang in languages if lang not in LANGUAGES]
        if invalid_languages:
            raise HTTPException(
                status_code=400,
                detail=f"지원하지 않는 대상 언어가 포함되어 있습니다: {', '.join(invalid_languages)}"
            )

        # NLLB로 번역 수행
        nllb_results = translate_nllb(
            text,
            LANGUAGES[source_language],
            [LANGUAGES[lang] for lang in languages]
        )
        
        # OpenAI로 재번역 수행
        translated_texts = {}
        for lang in languages:
            nllb_result = nllb_results[LANGUAGES[lang]]
            refined_text = translate_openai(text, nllb_result, lang)
            translated_texts[lang] = refined_text
        
        return {"translated_texts": translated_texts}
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"서버 오류 발생: {str(e)}")

@app.get("/")
async def root():
    return {"message": "Welcome to the NLLB + OpenAI multi-language translation API! Use /translate/ endpoint."}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, port=8000)