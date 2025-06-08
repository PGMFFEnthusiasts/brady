#!/bin/bash

DIR=$(readlink -f "$(dirname "$0")")
readonly DIR
cd "$DIR" || exit 1

if [ -f .env ]; then
  echo "✅ .env loaded"
  set -a
  source .env
  set +a
fi

export BOT_TOKEN

java -version 2>&1 | head -n 1 | grep -q " 21\." || { echo "❌ Use java 21" >&2; exit 1; }
java -Xms512M -Xmx4G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 \
 -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+DisableExplicitGC \
 -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M \
 -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 \
 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 \
 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -XX:UseAVX=2 -XX:+UseStringDeduplication \
 -XX:+UseFastUnorderedTimeStamps -XX:+UseAES -XX:+UseAESIntrinsics -XX:UseSSE=4 -XX:+UseFMA \
 -XX:AllocatePrefetchStyle=1 -XX:+UseLoopPredicate -XX:+RangeCheckElimination -XX:+EliminateLocks \
 -XX:+DoEscapeAnalysis -XX:+UseCodeCacheFlushing -XX:+SegmentedCodeCache -XX:+UseFastJNIAccessors \
 -XX:+OptimizeStringConcat -XX:+UseCompressedOops -XX:+UseThreadPriorities -XX:+OmitStackTraceInFastThrow \
 -XX:+TrustFinalNonStaticFields -XX:+UseInlineCaches -XX:+RewriteBytecodes -XX:+RewriteFrequentPairs \
 -XX:+UseNUMA -XX:-DontCompileHugeMethods -XX:+UseFPUForSpilling -XX:+UseFastStosb -XX:+UseNewLongLShift \
 -XX:+UseVectorCmov -XX:+UseXMMForArrayCopy -XX:+UseXmmI2D -XX:+UseXmmI2F -XX:+UseXmmLoadAndClearUpper \
 -XX:+UseXmmRegToRegMoveAll -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/urandom \
 --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED \
 --add-opens=java.base/java.lang.invoke=ALL-UNNAMED -jar sportpaper-1.8.8.jar nogui
