
# Google Cloud Vision API를 사용해서 OCR은 main에서는 사용되지 않음.
GOOGLE_APPLICATION_CREDENTIALS 키가 2개인데, (Server & OCR) 2개를 적용하려고 하니, 충돌되어 naver clova ocr을 사용하기로 결정.


# !!powershell 환경변수 적용하고 코드 실행.
1) openai api 환경변수:


setx OPENAI_API_KEY  "<your_openai_key"

==========================
2) google cloud vision api 환경변수:


$env:GOOGLE_APPLICATION_CREDENTIALS="your_google_service_account_json_file_path"


<<!!반드시 google cloud에서 발급받은 key값이 담긴 ~~.json파일이 있어야함.

