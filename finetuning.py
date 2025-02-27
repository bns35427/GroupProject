from transformers import AutoModelForSeq2SeqLM, AutoTokenizer, Seq2SeqTrainer, Seq2SeqTrainingArguments
import pandas as pd
from datasets import Dataset
import torch
import matplotlib.pyplot as plt
import os

# JSONL 파일 로드
data_path = '/workspace/finetuning/train_dataset.json'
try:
    df = pd.read_json(data_path)
except ValueError as e:
    raise ValueError("데이터 파일이 올바르지 않거나 경로가 잘못되었습니다.") from e

# 'Original'과 'BackTranslation' 필드를 사용하여 데이터셋 구성
data = Dataset.from_pandas(
    df[['Original', 'BackTranslation']].rename(columns={'Original': 'source', 'BackTranslation': 'target'})
)

# 전체 데이터셋을 훈련 데이터로 사용
train_data = data

# 모델 및 토크나이저 로드
model_name = "facebook/nllb-200-distilled-600M"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForSeq2SeqLM.from_pretrained(model_name, dropout=0.1)

# 토큰화 함수
def preprocess_function(examples):
    inputs = tokenizer(examples['source'], max_length=80, truncation=True, padding="max_length")
    targets = tokenizer(examples['target'], max_length=80, truncation=True, padding="max_length")
    inputs['labels'] = [[-100 if token == tokenizer.pad_token_id else token for token in target] 
                        for target in targets['input_ids']]
    return inputs

# 훈련 데이터 토큰화
tokenized_train_data = train_data.map(preprocess_function, batched=True)

# 학습 설정
training_args = Seq2SeqTrainingArguments(
    output_dir="./nllb_finetuned",
    evaluation_strategy="no",  # 평가 비활성화
    save_strategy="epoch",
    learning_rate=5e-5,
    per_device_train_batch_size=2,
    weight_decay=0.01,
    save_total_limit=2,
    num_train_epochs=3,
    predict_with_generate=True,
    logging_dir="./logs",
    logging_steps=20,
    fp16=True,
)

# 환경 변수 설정 (메모리 단편화 방지)
os.environ["PYTORCH_CUDA_ALLOC_CONF"] = "expandable_segments:True"

# 트레이너 설정
trainer = Seq2SeqTrainer(
    model=model,
    args=training_args,
    train_dataset=tokenized_train_data,
    tokenizer=tokenizer,
)

# 모델 학습
trainer.train()

# 훈련 손실 기록
train_losses = []
for log in trainer.state.log_history:
    if 'loss' in log:
        train_losses.append(log['loss'])

# 훈련 손실 그래프 시각화 및 파일 저장
plt.figure(figsize=(10, 5))
plt.plot(train_losses, label='Train Loss')
plt.xlabel('Steps')
plt.ylabel('Loss')
plt.title('Training Loss')
plt.legend()
plt.savefig('/workspace/finetuning/loss_plot.png')
plt.close()

# 모델 저장
model.save_pretrained("/workspace/finetuning/nllb_finetuned_model")
tokenizer.save_pretrained("/workspace/finetuning/nllb_finetuned_model")