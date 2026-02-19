import os
from typing import TypedDict
from langgraph.graph import StateGraph, END
from openai import OpenAI

client = OpenAI()

# ---------- STATE ----------
class QAState(TypedDict):
    qa_text: str
    report: str


# ---------- READ QA ----------
def read_qa(state: QAState):
    if not os.path.exists("release_qa.txt"):
        state["qa_text"] = "Q&A file missing."
        return state

    with open("release_qa.txt", "r", encoding="utf-8") as f:
        state["qa_text"] = f.read()

    return state


# ---------- ANALYZE QA ----------
def analyze_qa(state: QAState):

    prompt = f"""
You are a senior Android production readiness QA engineer for an offline music player.

Analyze the developer's release checklist answers below.

Goal:
- Detect REAL production risks only
- Identify crash/playback/billing risks
- Predict likely user complaints
- Decide if app is SAFE TO SHIP or FIX REQUIRED

Rules:
- Any NO in critical areas = RISK
- UNSURE = WARNING
- Focus on playback reliability, crashes, billing, background survival
- Ignore minor polish

Developer Answers:
{state['qa_text']}

Output in structured format:

CRASH & STABILITY → SAFE / RISK
PLAYBACK RELIABILITY → SAFE / RISK
BACKGROUND SERVICE → SAFE / RISK
NOTIFICATION & CONTROLS → SAFE / RISK
LIBRARY & STORAGE → SAFE / RISK
PERFORMANCE → OK / RISK
BILLING → SAFE / RISK
USER COMPLAINT RISK → LOW / MEDIUM / HIGH

FINAL DECISION → SAFE TO SHIP / FIX BEFORE RELEASE

Also list:
- Top 3 risks (if any)
- What MUST be fixed before release
"""

    response = client.responses.create(
        model="gpt-4.1-mini",
        input=prompt
    )

    state["report"] = response.output_text
    return state


# ---------- SAVE REPORT ----------
def save_report(state: QAState):
    os.makedirs("qa_output", exist_ok=True)
    with open("qa_output/qa_decision.txt", "w", encoding="utf-8") as f:
        f.write(state["report"])

    print("\nQA Decision saved → qa_output/qa_decision.txt\n")
    return state


# ---------- GRAPH ----------
graph = StateGraph(QAState)
graph.add_node("read_qa", read_qa)
graph.add_node("analyze_qa", analyze_qa)
graph.add_node("save_report", save_report)

graph.set_entry_point("read_qa")
graph.add_edge("read_qa", "analyze_qa")
graph.add_edge("analyze_qa", "save_report")
graph.add_edge("save_report", END)

app = graph.compile()


# ---------- RUN ----------
if __name__ == "__main__":
    initial_state = {"qa_text": "", "report": ""}
    app.invoke(initial_state)
