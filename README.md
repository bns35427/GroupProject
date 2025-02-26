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
### 3) Server : 
(1)설치 패키지 : requirements.txt에 있음.
### 4) App :
# 모바일 개발

1. 기술스택:안드로이드 스튜디오,Java,xml


2-1.Firebase 프로젝트 생성
2-2.파이어베이스 json데이터 발급 및 프로젝트에 저장
2-3.Firebase Authentication 활성화
2-4.로그인 방법(이메일/비밀번호) 활성화

3.naver Map API —> Naver Cloud Platform 접속, 키 발급(Dynamic Mobile Map,Gecoding 활성화)

4.firestore 활성화

(그 외에 환경설정 관련 부분들은 안드로이드 스튜디오에 경우에 Dependency,AndroidManifest.xml에 코드로 저장)

###

## 코드 실행법

## 실행 예시

