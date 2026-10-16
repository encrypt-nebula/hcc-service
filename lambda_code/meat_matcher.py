import re

NEGATIVE_PATTERNS = [
    re.compile(r'\b(?:history\s+of|hx\s+of|family\s+history|fhx|rule\s+out|r/o|possible|suspected|screening\s+only|denies|no\s+evidence\s+of|negative\s+for)\b', re.IGNORECASE)
]

MONITORING_TERMS = [
    "reviewed", "monitor", "monitored", "monitoring", "follow-up", "recheck",
    "trend", "tracking", "surveillance", "watchful", "observe", "observation",
    "vitals reviewed", "bp monitored", "blood pressure check", "pulse check",
    "weight monitored", "oxygen saturation", "pulse ox", "temperature monitored",
    "glucose log", "sugar log", "a1c reviewed", "creatinine reviewed", "gfr reviewed",
    "lipid panel reviewed", "cholesterol reviewed", "cbc reviewed", "cmp reviewed",
    "bmp reviewed", "urine test reviewed", "microalbumin reviewed", "thyroid test reviewed",
    "psa reviewed", "troponin reviewed", "bnp reviewed", "liver enzymes reviewed",
    "repeat labs", "repeat imaging", "repeat test", "next visit", "scheduled follow-up",
    "return in 3 months", "return in 6 months", "follow-up visit", "telehealth follow-up",
    "monitor symptoms", "symptom diary", "pain scale monitored", "mobility monitored",
    "fall risk monitored", "diet log reviewed", "exercise log reviewed", "bp log reviewed",
    "home glucose monitoring", "home bp monitoring", "device monitoring", "remote monitoring",
    "holter monitoring", "cardiac monitoring", "oxygen monitoring", "cpap compliance monitored",
    "dialysis monitoring", "fluid balance monitored", "wound monitored", "ulcer monitored",
    "mental status monitored", "memory monitored", "medication compliance monitored",
    "adherence monitored", "side effects monitored", "tolerance monitored", "response monitored",
    "healing monitored", "recovery monitored", "respiratory status monitored", "renal function monitored"
]

EVALUATION_TERMS = [
    "ordered", "evaluation", "evaluate", "assess further", "diagnostic test",
    "lab ordered", "cbc ordered", "cmp ordered", "bmp ordered", "a1c ordered",
    "lipid panel ordered", "thyroid test ordered", "psa ordered", "troponin ordered",
    "bnp ordered", "blood culture ordered", "urine culture ordered", "covid test ordered",
    "imaging ordered", "x-ray ordered", "ct scan ordered", "mri ordered", "ultrasound ordered",
    "doppler ordered", "echo ordered", "stress test ordered", "angiogram ordered",
    "ekg ordered", "holter ordered", "biopsy ordered", "colonoscopy ordered",
    "endoscopy ordered", "bronchoscopy ordered", "screening test", "cancer screening",
    "mammogram ordered", "referral", "consult", "specialist referral",
    "cardiology referral", "nephrology referral", "neurology referral", "oncology referral",
    "endocrinology referral", "pulmonology referral", "gi referral", "orthopedic referral",
    "psychiatry referral", "pain management referral", "pt referral", "ot referral",
    "speech therapy referral", "sleep study ordered", "nerve conduction study",
    "emg ordered", "urology evaluation", "ophthalmology exam", "hearing test",
    "vision test", "bone density test", "dexascan ordered", "cardiac enzymes",
    "inr test", "pt ptt test", "toxicology screen", "spirometry ordered"
]

ASSESSMENT_TERMS = [
    "stable", "worsening", "improving", "improved", "controlled", "uncontrolled",
    "poorly controlled", "well controlled", "exacerbation", "acute exacerbation",
    "chronic", "acute", "progression", "resolved", "in remission", "partial remission",
    "relapse", "recurrence", "stage 1", "stage 2", "stage 3", "stage 4", "stage 5",
    "risk high", "risk low", "risk moderate", "complicated", "uncomplicated",
    "severe", "mild", "moderate", "critical", "terminal", "metastatic", "advanced",
    "early stage", "new diagnosis", "confirmed", "probable", "definite",
    "functional decline", "cognitive decline", "neuropathy present", "retinopathy present",
    "nephropathy present", "ulcer present", "infection present", "sepsis present",
    "renal failure", "heart failure", "respiratory failure", "liver failure",
    "chronic pain", "acute pain", "bleeding controlled", "bleeding uncontrolled",
    "anemia severe", "edema present", "ascites present", "effusion present",
    "arrhythmia present", "tachycardia present", "hypertension controlled",
    "hypertension uncontrolled", "diabetes controlled", "diabetes uncontrolled",
    "ckd stage 3", "ckd stage 4", "ckd stage 5", "copd stable", "copd exacerbation",
    "asthma controlled", "asthma uncontrolled", "infection resolved"
]

TREATMENT_TERMS = [
    "started", "prescribed", "continue", "continued", "adjusted", "increased dose",
    "decreased dose", "added medication", "stopped medication", "changed medication",
    "switched medication", "insulin started", "metformin started", "statin started",
    "beta blocker started", "ace inhibitor started", "diuretic started", "antibiotic started",
    "pain medication started", "steroid started", "inhaler started", "oxygen started",
    "cpap started", "dialysis started", "chemotherapy started", "radiation started",
    "immunotherapy started", "surgery planned", "surgery performed", "procedure done",
    "biopsy done", "stent placed", "pacemaker placed", "catheter placed",
    "wound dressing applied", "pt ordered", "ot ordered", "speech therapy ordered",
    "home health ordered", "nutrition plan started", "diet changed", "exercise program started",
    "dose titrated", "dose tapered", "dose held", "maintenance therapy", "therapy ordered",
    "physical therapy ordered", "vaccination given"
]

def _compile_terms(terms):
    patterns = []
    for t in terms:
        escaped = re.escape(t.strip())
        pattern = re.compile(r'\b' + escaped.replace(r'\ ', r'\s+') + r'\b', re.IGNORECASE)
        patterns.append((t.strip().lower(), pattern))
    return patterns

COMPILED_MONITORING = _compile_terms(MONITORING_TERMS)
COMPILED_EVALUATION = _compile_terms(EVALUATION_TERMS)
COMPILED_ASSESSMENT = _compile_terms(ASSESSMENT_TERMS)
COMPILED_TREATMENT = _compile_terms(TREATMENT_TERMS)

def extract_meat_from_text(text):
    if not text:
        return {
            "monitor": False,
            "evaluate": False,
            "assessOrAddress": False,
            "treat": False,
            "monitoringKeywords": [],
            "evaluationKeywords": [],
            "assessmentKeywords": [],
            "treatmentKeywords": []
        }

    lines = re.split(r'[\r\n]+|[.;]', text)
    
    monitoring_found = set()
    evaluation_found = set()
    assessment_found = set()
    treatment_found = set()

    for line in lines:
        line_clean = line.strip()
        if not line_clean:
            continue
        
        if any(neg.search(line_clean) for neg in NEGATIVE_PATTERNS):
            continue

        for term, pattern in COMPILED_MONITORING:
            if pattern.search(line_clean):
                monitoring_found.add(term)

        for term, pattern in COMPILED_EVALUATION:
            if pattern.search(line_clean):
                evaluation_found.add(term)

        for term, pattern in COMPILED_ASSESSMENT:
            if pattern.search(line_clean):
                assessment_found.add(term)

        for term, pattern in COMPILED_TREATMENT:
            if pattern.search(line_clean):
                treatment_found.add(term)

    return {
        "monitor": len(monitoring_found) > 0,
        "evaluate": len(evaluation_found) > 0,
        "assessOrAddress": len(assessment_found) > 0,
        "treat": len(treatment_found) > 0,
        "monitoringKeywords": sorted(list(monitoring_found)),
        "evaluationKeywords": sorted(list(evaluation_found)),
        "assessmentKeywords": sorted(list(assessment_found)),
        "treatmentKeywords": sorted(list(treatment_found))
    }
