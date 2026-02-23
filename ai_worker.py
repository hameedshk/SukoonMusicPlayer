import subprocess
import time
from pathlib import Path
from datetime import datetime

# =========================
# CONFIG
# =========================

PROJECT_PATH = "."
BUILD_SCRIPT = "smart_run.ps1"

TASKS_ROOT = Path("ai_tasks")
PENDING_DIR = TASKS_ROOT / "pending"
IN_PROGRESS_DIR = TASKS_ROOT / "in_progress"
DONE_DIR = TASKS_ROOT / "done"
FAILED_DIR = TASKS_ROOT / "failed"

# =========================
# TASK DIRECTORY SETUP
# =========================

def ensure_task_dirs():
    for d in [PENDING_DIR, IN_PROGRESS_DIR, DONE_DIR, FAILED_DIR]:
        d.mkdir(parents=True, exist_ok=True)

# =========================
# TASK HANDLING (FOLDER-BASED)
# =========================

def get_next_task_file():
    files = sorted(PENDING_DIR.glob("*.md"))
    if not files:
        return None
    return files[0]

def move_task(src_path, target_dir):
    target_path = target_dir / src_path.name
    src_path.rename(target_path)
    return target_path

def read_task(task_path):
    return task_path.read_text(encoding="utf-8")

# =========================
# CODEX CALL
# =========================

def run_codex(prompt):
    print("\n[CODEX TASK]\n")

    result = subprocess.run(
        [
            r"C:\Users\ksham\AppData\Roaming\npm\codex.cmd",
            "exec",
            "--full-auto",
            "--sandbox",
            "workspace-write",
            "--cd",
            PROJECT_PATH
        ],
        input=prompt,        # <-- VERY IMPORTANT
        text=True
    )

    if result.returncode != 0:
        print("[ERROR] Codex execution failed.")
        return False

    return True

# =========================
# BUILD USING smart_run.ps1
# =========================

def build_project():
    print("\n[BUILD] Running smart_run.ps1...\n")

    result = subprocess.run(
        [
            "powershell",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            BUILD_SCRIPT
        ],
        cwd=PROJECT_PATH,
        capture_output=True,
        text=True
    )

    return result.returncode, result.stdout + result.stderr

# =========================
# FIX BUILD ERRORS
# =========================

def fix_build_with_codex(error_log):
    print("\n[CODEX FIXING BUILD ERROR]\n")

    trimmed_log = error_log[-1000:]

    prompt = f"""
Android Kotlin project build/deploy failed.

Fix ONLY the real compile/runtime/build errors from the log below.

Rules:
- Minimal safe changes
- Do NOT refactor unrelated code
- Do NOT break existing features
- Modify build.gradle ONLY if error explicitly requires dependency/plugin fix

ERROR LOG:
{trimmed_log}
"""

    return run_codex(prompt)

# =========================
# MAIN LOOP
# =========================

def main_loop():
    ensure_task_dirs()

    print("\n=== AI WORKER STARTED (24x7) ===\n")

    while True:
        task_file = get_next_task_file()

        if not task_file:
            print("No pending tasks. Sleeping 5 minutes...")
            time.sleep(300)
            continue

        print(f"\n========== NEW TASK ==========\n{task_file.name}\n")

        task_file = move_task(task_file, IN_PROGRESS_DIR)
        task_content = read_task(task_file)

        task_start_time = datetime.now()

        # Step 1 — Implement task
        success = run_codex(f"""
Android Kotlin project.

Implement this task:
{task_content}

Rules:
- Production-ready code
- Minimal safe changes
- Do not break existing features
- Modify build.gradle ONLY if required
""")

        if not success:
            print("[ERROR] Task implementation failed.")
            move_task(task_file, FAILED_DIR)
            continue

        # Step 2 — Build + Fix loop
        build_success = False

        for attempt in range(5):
            print(f"\n[BUILD ATTEMPT {attempt+1}/5]\n")

            code, output = build_project()

            if code == 0:
                build_success = True
                break

            fix_success = fix_build_with_codex(output)

            if not fix_success:
                break

            time.sleep(2)

        if build_success:
            print("\n[SUCCESS] Task completed\n")
            move_task(task_file, DONE_DIR)
        else:
            print("\n[FAILED AFTER RETRIES]\n")
            move_task(task_file, FAILED_DIR)

        time.sleep(5)

# =========================
# ENTRY
# =========================

if __name__ == "__main__":
    main_loop()