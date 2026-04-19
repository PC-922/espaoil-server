#!/usr/bin/env sh
# Live reload watcher — rebuild cuando hay cambios en src/
# Más rápido que gradle -t, sin esperar full build

set -e

SRC_DIR="/app/app/src/main/kotlin"
BUILD_DIR="/app/app/build"
LAST_CHECKSUM=""

compute_checksum() {
	find "$SRC_DIR" -type f -name "*.kt" | xargs cat | md5sum | awk '{print $1}'
}

rebuild_and_restart() {
	echo "[$(date '+%H:%M:%S')] Detected changes, recompiling..."

	cd /app
	gradle compileKotlin --no-daemon 2>&1 | tail -3 || {
		echo "[$(date '+%H:%M:%S')] Compile error — waiting for fixes..."
		return 1
	}

	echo "[$(date '+%H:%M:%S')] ✅ Recompiled successfully"
}

run_app() {
	cd /app
	gradle run --no-daemon
}

echo "[$(date '+%H:%M:%S')] Starting app with live reload..."
run_app &
APP_PID=$!

while true; do
	sleep 1
	CURRENT_CHECKSUM=$(compute_checksum)

	if [ "$CURRENT_CHECKSUM" != "$LAST_CHECKSUM" ]; then
		LAST_CHECKSUM="$CURRENT_CHECKSUM"

		# Kill old app
		kill $APP_PID 2>/dev/null || true
		wait $APP_PID 2>/dev/null || true

		# Rebuild + restart
		if rebuild_and_restart; then
			run_app &
			APP_PID=$!
		fi
	fi
done
