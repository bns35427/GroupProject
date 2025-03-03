# 이 프로젝트는 한국에 있는 이주노동자를 돕기 위해 시작되었습니다. 

## 개발 환경 :
### 1) OCR :
(1)Visual studio code
### 2) Finetunning :
(1)Visual studio code
### 3) Server :
(1)Visual studio code

(2)Google cloud CLI<<cloud server에 올리기 위해 설치하는 것이기에, 로컬로 돌릴 예정이라면 설치하지 않아도 됨.

Google cloud CLI 설치 링크 : <https://cloud.google.com/sdk/docs/install-sdk?hl=ko)>

(2)-1
google cloud CLI(=gcloud CLI) 설치하기 전, 설정사항이 있음.

Google Cloud project를 생성하고,

<https://console.cloud.google.com/projectselector2/home/dashboard?inv=1&invt=Abqj6A> 

그 프로젝트에 결제 계정을 연결해아함.

<https://cloud.google.com/billing/docs/how-to/verify-billing-enabled?hl=ko#confirm_billing_is_enabled_on_a_project>

위의 내용은 아래 링크의 가이드에 나와있기에, 가이드대로 따라하면 됨.

<https://cloud.google.com/sdk/docs/install-sdk?hl=ko)>

(3)Docker Desktop

### 4) App :
(1)안드로이드 스튜디오


## 코드 실행 전, 세팅(필요한 환경 및 설정사항)

### 1) OCR :
(1)설치 패키지 : requirements.txt에 있음.

(2) CLOVA OCR :
1. <https://www.ncloud.com/>의 상단에 있는 "콘솔" 클릭.
2. 왼쪽의 "Service"탭에서 CLOVA OCR & API Gateway 검색한 뒤, 각각 서비스 신청.
3. API Gateway의 My Products에서 "Product 생성" 클릭한 뒤, 생성.
4. CLOVA OCR은 "일반/템플릿" 도메인 생성.
5. CLOVA OCR의 "Domain"에서 "API Gateway 연동" 클릭후, API Gateway 자동 연동의 "수정하기" 클릭.
### 2) Finetunning :
(1)설치 패키지 : requirements.txt에 있음.


(1)-1**설치 패키지 및 연결 실행 방법:**

1. nltk
        - 문장 분할을 위한 패키지
        - 설치 명령어:
            
            ```python
               pip install nltk
            ```
            
    
    b. transformers
    
    - Hugging Face의 AutoModelForSeq2SeqLM과 AutoTokenizer 이용을 위한 패키지
    - NLLB-200 모델 로드
    - 설치 명령어:
        
        ```python
        pip install transformers
        ```
        
    
    c. PyTorch
    
    - transformers 라이브러리가 모델 실행하기 위한 백엔드 패키지
    - GPU를 사용하므로 CUDA 지원 버전 설치 필요
    - 설치 명령어:
        
        ```python
        pip3 install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu126
        ```
        
    
    d. fastapi
    
    - FastAPI 프레임워크 사용, API 서버 구축
    - 설치 명령어:
        
        ```python
                 pip install fastapi
        ```
        
    
    e. pydantic
    
    - 데이터 검증 및 요청, 응답 모델 정의 패키지
    - 설치 명령어:
        
        ```python
        pip install pydantic
        ```
        
    
    f. openai
    
    - OpenAI API 호출해 NLLB 번역을 맥락에 맞게 재번역
    - 설치 명령어:
        
        ```python
        pip install openai
        ```
        
    
    g. uvicorn
    
    - FastAPI를 실행하기 위한 ASGI 서버
    - 설치 명령어:
        
        ```python
        pip install uvicorn
        ```
        
    - 코드 내에 os.getenv(”OPENAI_API_KEY”)를 사용하므로 OpenAI API 키를 환경변수로 설정하는 과정이 추가적으로 요구됨.
        - 환경변수 작업을 직접 진행해도 되지만 코드로 작성하여도 가능
            - Windows:
                
                ```python
                set OPENAI_API_KEY=your-openai-api-key
                ```
                
            - Linux/Mac:
                
                ```python
                export OPENAI_API_KEY=your-openai-api-key
                ```
                
    
    h. Ngrok
    
    - 다른 로컬 서버에서 Fastapi 서버에 접근할 수 있도록 사용
    - 실행법:
        1. 자신의 os에 맞는 Ngrok.exe 다운(https://ngrok.com/download)
        2. Ngrok 회원가입 후 토큰 발급
        (https://dashboard.ngrok.com/get-started/setup/windows)
        3. Ngrok.exe 실행하여 발급된 토큰을 등록
            1. 명령어:
                
                ```python
                ngrok config add-authtoken '본인 토큰'
                ```
                
        4. fastapi를 실행한 포트와 동일한 포트번호를 입력하여 서버 오픈
            1. 명령어:
                
                ```python
                ngrok http '포트번호'
                ```
                
        5. Forwarding의 https://~~.ngrok.io 주소로 외부에서 접근 가능 
            - 요청받은 내용을 확인하고자 한다면 Web Interface의 링크에서 확인 가능
              
(2)번역 서비스 파인튜닝을 위한 GPU 클라우드 사용법

1. [vast.ai](http://vast.ai) 회원가입
2. puTTy, winSCP(다른 방법시 복잡하고 오류가 많음) 설치
3. SSH 접속을 위해 `ssh-keygen`으로 비공개키와 공개키 쌍을 생성한 뒤, 공개키를 Vast.ai에 등록해요. (보안을 위해 키에 비밀번호를 추가하는 걸 추천)
4. [vast.ai](http://vast.ai) 홈페이지에서 account-cloud connection로 구글클라우드 동기화
5. vast.ai에서 인스턴스 추가
6. 구글 클라우드에 실행시킬 폴더(파인튜닝코드, 데이터셋, requirements.txt) 업로드
7. vast.ai와 동기화(인스턴스에 클라우드 버튼 누르면 됨. 경로 올바르게)
8. ssh연결버튼(키모양) 누르면 이런식으로 보임(ex ssh -p 8130 [root@82.141.118.](mailto:root@82.141.118.2)6 -L 8080:localhost:8080) 앞에 8130이 포트번호, 뒤에82.141.118.6이 IP address
9. ssh접속 위해 puTTy접속(아까 생성한 ppk사용)
10. 로그인 시 사용자 “root” 입력 비번 설정 시 입력
11. cd (클라우드에올린파일경로) 로 폴더 안으로 들어가기
12. pip install -r requiements.txt 로 필요한 파이썬 패키지를 설치
13. python [파일명.py](http://파일명.py) 로 파인튜닝진행
14. 40분정도 기다리면 결과 출력 및 모델 저장
15. winSCP 접속해서 SFTP로 똑같이 접속 후 파일 옮겨서 모델 가져오기

### 3) Server : 
(1)설치 패키지 : requirements.txt에 있음.
### 4) App :
# 모바일 개발
기술스택:안드로이드 스튜디오,Java,xml

1. Firebase 프로젝트 생성 --> 파이어베이스 json데이터 발급 및 프로젝트에 저장 --> Firebase Authentication 활성화 --> 로그인 방법(이메일/비밀번호) 활성화

2. naver Map API —> Naver Cloud Platform 접속, 키 발급(Dynamic Mobile Map,Gecoding 활성화)

3. firestore 활성화

(그 외에 환경설정 관련 부분들은 안드로이드 스튜디오에 경우에 Dependency,AndroidManifest.xml에 코드로 저장)

###

## 코드 실행법

## 실행 예시

