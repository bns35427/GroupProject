


def detect_text(path):
    """Detects text in the file and extracts sentence boundaries with GPT."""
    from google.cloud import vision
    from openai import OpenAI
    import json

    # 1. Google Vision API로 텍스트 추출
    vision_client = vision.ImageAnnotatorClient()
    with open(path, "rb") as image_file:
        content = image_file.read()
    image = vision.Image(content=content)
    response = vision_client.text_detection(image=image)
    texts = response.text_annotations

    if response.error.message:
        raise Exception(
            f"{response.error.message}\nFor more info: https://cloud.google.com/apis/design/errors"
        )

    # 2. 전체 텍스트 추출 (texts[0].description)
    full_text = texts[0].description if texts else ""


    #!!생각보다 gpt한테 메세지 간단하게만 보내도, google vision api 성능이 좋아서 그런지, 결과값 나쁘지 않은디?!
    # 3. GPT API로 문장 분리 (JSON 형식 요청)
    openai_client = OpenAI()
    gpt_response = openai_client.chat.completions.create(
        model="gpt-4-1106-preview",  # JSON 모드 지원 버전
        messages=[
            {
                "role": "system",
                "content": (
                    "너는 텍스트를 문장 단위로 분리하는 도우미야. "
                    "응답은 반드시 아래 JSON 형식을 따라야 해:\n"
                    '{"sentences": ["문장1", "문장2", ...]}'
                )
            },
            {
                "role": "user",
                "content": f"다음 텍스트를 문장별로 분리해줘:\n\n{full_text}"
            }
        ],
        response_format={  # 파라미터 단순화
            "type": "json_object"
        }
    )

    # 4. GPT 응답 파싱
    gpt_output = json.loads(gpt_response.choices[0].message.content)
    gpt_sentences = gpt_output.get("sentences", [])

    # 5. 문장별 시작/끝 단어 바운딩 박스 매핑
    # 5. 문장별 시작/끝 단어 바운딩 박스 매핑 (인덱스 기반)
    output = []
    current_index = 1  # texts[0]은 전체 텍스트이므로 1부터 시작

    for sentence in gpt_sentences:
        start_word = None
        end_word = None

        # 시작 단어 찾기 (현재 인덱스 이후부터 검색)
        for idx, text in enumerate(texts[current_index:], start=current_index):
            if sentence.startswith(text.description.strip()):
                start_word = text
                current_index = idx + 1  # 다음 검색 시작 위치 업데이트
                break

        # 끝 단어 찾기 (시작 단어 이후부터 검색)
        if start_word:
            for idx, text in enumerate(texts[current_index:], start=current_index):
                if sentence.endswith(text.description.strip()):
                    end_word = text
                    current_index = idx + 1  # 다음 검색 시작 위치 업데이트
                    break

        # 결과 저장
        if start_word and end_word:
            output.append({
                "sentence": sentence,
                "start_word": {
                    "text": start_word.description,
                    "bounds": [(v.x, v.y) for v in start_word.bounding_poly.vertices]#??start_word에서는 bounds값 1&4만 추출하면 될듯?
                },
                "end_word": {
                    "text": end_word.description,
                    "bounds": [(v.x, v.y) for v in end_word.bounding_poly.vertices]#??end_word에서는 bounds값 2&3만 추출하면 될듯?
                }
            })

    # 6. 최종 결과 출력
    print(json.dumps({"sentences": output}, indent=2, ensure_ascii=False))



#사진 경로 바꾸면 됨.
detect_text('ocr_test.jpg')