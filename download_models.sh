#!/bin/bash
# 易达熊 MediaPipe 模型文件下载脚本
set -e

ASSETS_DIR="app/src/main/assets"
BASE_URL="https://storage.googleapis.com/mediapipe-models"

echo "=================================="
echo "  易达熊 模型文件下载"
echo "=================================="

mkdir -p "$ASSETS_DIR"

echo "[1/3] 下载 face_landmarker.task (~6MB)..."
curl -L -o "$ASSETS_DIR/face_landmarker.task" \
  "$BASE_URL/face_landmarker/face_landmarker/float16/latest/face_landmarker.task"
echo "  ✅ 完成"

echo "[2/3] 下载 pose_landmarker_lite.task (~4MB)..."
curl -L -o "$ASSETS_DIR/pose_landmarker_lite.task" \
  "$BASE_URL/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task"
echo "  ✅ 完成"

echo "[3/3] 下载 hand_landmarker.task (~5MB)..."
curl -L -o "$ASSETS_DIR/hand_landmarker.task" \
  "$BASE_URL/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task"
echo "  ✅ 完成"

echo ""
echo "全部下载完成！"
ls -lh "$ASSETS_DIR"/*.task 2>/dev/null
