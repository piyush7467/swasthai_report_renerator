import os
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from pptx.dml.color import RGBColor

def create_presentation():
    prs = Presentation()
    # 16:9 Widescreen layout
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)
    blank_layout = prs.slide_layouts[6] # completely blank slide

    assets_dir = r"d:/Java-spring-boot-projects/swasthai-report-generator/presentation_assets"
    
    # Palette definition (Clean, Light Theme)
    BG_WHITE = RGBColor(255, 255, 255)
    BG_CARD = RGBColor(248, 250, 252) # Slate-50
    BORDER_CARD = RGBColor(226, 232, 240) # Slate-200
    BORDER_TEAL = RGBColor(204, 251, 241) # Teal-100
    
    COLOR_TEAL = RGBColor(13, 148, 136) # Teal-600
    COLOR_DARK_TEAL = RGBColor(15, 118, 110) # Teal-700
    COLOR_HEADING = RGBColor(15, 23, 42) # Slate-900
    COLOR_BODY = RGBColor(71, 85, 105) # Slate-600
    COLOR_MUTED = RGBColor(100, 116, 139) # Slate-500
    
    COLOR_RED = RGBColor(225, 29, 72) # Rose-600
    COLOR_GREEN = RGBColor(22, 163, 74) # Green-600
    COLOR_BLUE = RGBColor(2, 132, 199) # Sky-600
    COLOR_AMBER = RGBColor(217, 119, 6) # Amber-600

    def add_header(slide, tag_text, title_text, subtitle_text=None):
        # Category Tag badge
        tag_box = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(0.4), Inches(2.2), Inches(0.35))
        tag_box.fill.solid()
        tag_box.fill.fore_color.rgb = RGBColor(240, 253, 250)
        tag_box.line.color.rgb = COLOR_TEAL
        tag_box.line.width = Pt(1)
        tf_tag = tag_box.text_frame
        tf_tag.vertical_anchor = MSO_ANCHOR.MIDDLE
        p_tag = tf_tag.paragraphs[0]
        p_tag.text = tag_text.upper()
        p_tag.alignment = PP_ALIGN.CENTER
        p_tag.font.bold = True
        p_tag.font.size = Pt(9.5)
        p_tag.font.color.rgb = COLOR_DARK_TEAL

        # Title
        tb = slide.shapes.add_textbox(Inches(0.8), Inches(0.8), Inches(11.7), Inches(0.7))
        tf = tb.text_frame
        tf.word_wrap = True
        p = tf.paragraphs[0]
        p.text = title_text
        p.font.bold = True
        p.font.size = Pt(22)
        p.font.color.rgb = COLOR_HEADING

        if subtitle_text:
            p2 = tf.add_paragraph()
            p2.text = subtitle_text
            p2.font.size = Pt(12)
            p2.font.color.rgb = COLOR_BODY

    def add_card(slide, left, top, width, height, bg_color=BG_CARD, border_color=BORDER_CARD):
        card = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, width, height)
        card.fill.solid()
        card.fill.fore_color.rgb = bg_color
        card.line.color.rgb = border_color
        card.line.width = Pt(1.2)
        return card

    # =========================================================================
    # SLIDE 1: Title & Hero
    # =========================================================================
    slide1 = prs.slides.add_slide(blank_layout)
    # Background subtle wash
    bg1 = slide1.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(13.333), Inches(7.5))
    bg1.fill.solid()
    bg1.fill.fore_color.rgb = RGBColor(250, 252, 253)
    bg1.line.fill.background()

    # Logo
    logo_path = os.path.join(assets_dir, "swasthai_logo.png")
    if os.path.exists(logo_path):
        slide1.shapes.add_picture(logo_path, Inches(0.8), Inches(0.8), height=Inches(0.85))

    # Title & Subtitle in Left Column
    tbox = slide1.shapes.add_textbox(Inches(0.8), Inches(2.0), Inches(7.5), Inches(3.2))
    tf1 = tbox.text_frame
    tf1.word_wrap = True
    
    p = tf1.paragraphs[0]
    p.text = "SWASTHAI REPORT GENERATOR"
    p.font.bold = True
    p.font.size = Pt(32)
    p.font.color.rgb = COLOR_HEADING

    p2 = tf1.add_paragraph()
    p2.text = "Automated Pathology Laboratory Information & Certified Diagnostic Reporting System"
    p2.font.bold = True
    p2.font.size = Pt(16)
    p2.font.color.rgb = COLOR_TEAL

    p3 = tf1.add_paragraph()
    p3.space_before = Pt(14)
    p3.text = "A modern, full-stack Android & Spring Boot clinical solution bridging the gap between raw hardware analyzer data and transparent, patient-centric diagnostic intelligence."
    p3.font.size = Pt(13)
    p3.font.color.rgb = COLOR_BODY

    # Meta / Presenter Info card
    pcard = add_card(slide1, Inches(0.8), Inches(5.6), Inches(7.0), Inches(1.3), bg_color=RGBColor(255, 255, 255), border_color=BORDER_CARD)
    tb_meta = slide1.shapes.add_textbox(Inches(1.0), Inches(5.7), Inches(6.6), Inches(1.1))
    tf_meta = tb_meta.text_frame
    m1 = tf_meta.paragraphs[0]
    m1.text = "Project Lead & Developer: Piyush Shakya"
    m1.font.bold = True
    m1.font.size = Pt(13)
    m1.font.color.rgb = COLOR_HEADING
    m2 = tf_meta.add_paragraph()
    m2.text = "Architecture: Android Jetpack Compose (Kotlin) • Spring Boot • PostgreSQL REST API"
    m2.font.size = Pt(11)
    m2.font.color.rgb = COLOR_BODY
    m3 = tf_meta.add_paragraph()
    m3.text = "Target Domain: Clinical Diagnostic Centers • Pathology Labs • Healthcare Transparency"
    m3.font.size = Pt(11)
    m3.font.color.rgb = COLOR_TEAL

    # Right Hero Image: App Dashboard Screenshot with sleek frame
    app_dash = os.path.join(assets_dir, "app_dashboard.jpg")
    if os.path.exists(app_dash):
        # Decorative frame
        fcard = add_card(slide1, Inches(8.8), Inches(1.0), Inches(3.8), Inches(5.8), bg_color=RGBColor(255, 255, 255), border_color=COLOR_TEAL)
        slide1.shapes.add_picture(app_dash, Inches(9.0), Inches(1.2), width=Inches(3.4))

    # =========================================================================
    # SLIDE 2: Problem Statement (The Lab Bottleneck)
    # =========================================================================
    slide2 = prs.slides.add_slide(blank_layout)
    add_header(slide2, "Current Industry Crisis", "The Real-World Pathology Bottleneck", "Why traditional diagnostic workflows fail technicians, doctors, and patients.")

    # 3 Column Cards for the 3 Key Problems
    problems = [
        ("1. Cryptic Hardware Data", 
         "Hematology analyzers (like MISPA CountX) output complex numeric codes, abbreviations (WBC, MCV, PLCR), and raw histograms. Non-specialists and patients find this completely incomprehensible.", 
         COLOR_RED, "❌ Raw Machine Jargon"),
        ("2. Manual Typing & Math Errors", 
         "Technicians manually transcribe numbers from analyzer screens to Word/Excel. Manual calculation of absolute counts and ratios causes high transcription error rates and dangerous diagnostic mistakes.", 
         COLOR_AMBER, "❌ Error-Prone Transcription"),
        ("3. Delayed Turnaround & Security Risks", 
         "Patients wait hours or days for paper printouts. Physical paper reports get misplaced, torn, or easily forged without any cryptographic verification or tamper protection.", 
         COLOR_RED, "❌ Fraud & Extreme Delays")
    ]

    for idx, (title, desc, accent, badge) in enumerate(problems):
        x = Inches(0.8 + idx * 4.0)
        c = add_card(slide2, x, Inches(1.8), Inches(3.7), Inches(4.8), bg_color=RGBColor(255, 255, 255))
        
        # Badge
        badge_box = slide2.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x + Inches(0.3), Inches(2.1), Inches(3.1), Inches(0.4))
        badge_box.fill.solid()
        badge_box.fill.fore_color.rgb = RGBColor(254, 242, 242) if accent == COLOR_RED else RGBColor(254, 243, 199)
        badge_box.line.color.rgb = accent
        p_b = badge_box.text_frame.paragraphs[0]
        p_b.text = badge
        p_b.alignment = PP_ALIGN.CENTER
        p_b.font.bold = True
        p_b.font.size = Pt(11)
        p_b.font.color.rgb = accent

        tb_c = slide2.shapes.add_textbox(x + Inches(0.3), Inches(2.7), Inches(3.1), Inches(3.7))
        tfc = tb_c.text_frame
        tfc.word_wrap = True
        p_t = tfc.paragraphs[0]
        p_t.text = title
        p_t.font.bold = True
        p_t.font.size = Pt(16)
        p_t.font.color.rgb = COLOR_HEADING

        p_d = tfc.add_paragraph()
        p_d.space_before = Pt(12)
        p_d.text = desc
        p_d.font.size = Pt(12.5)
        p_d.font.color.rgb = COLOR_BODY

    # =========================================================================
    # SLIDE 3: The Core Visual Transformation (Before vs After)
    # =========================================================================
    slide3 = prs.slides.add_slide(blank_layout)
    add_header(slide3, "The Transformation", "From Cryptic Hardware Screen to Certified Medical Report", "Side-by-side comparison of analyzer output versus SwasthAI generated intelligence.")

    # Left: Raw Machine
    add_card(slide3, Inches(0.8), Inches(1.8), Inches(5.6), Inches(5.2), bg_color=RGBColor(254, 242, 242), border_color=COLOR_RED)
    tb_m = slide3.shapes.add_textbox(Inches(1.0), Inches(1.9), Inches(5.2), Inches(0.6))
    p_m = tb_m.text_frame.paragraphs[0]
    p_m.text = "BEFORE: Raw Hematology Analyzer Output (MISPA CountX)"
    p_m.font.bold = True
    p_m.font.size = Pt(13)
    p_m.font.color.rgb = COLOR_RED
    
    machine_img = os.path.join(assets_dir, "machine_raw_screen.jpg")
    if os.path.exists(machine_img):
        slide3.shapes.add_picture(machine_img, Inches(1.0), Inches(2.5), width=Inches(5.2))
        
    tb_m_desc = slide3.shapes.add_textbox(Inches(1.0), Inches(5.7), Inches(5.2), Inches(1.2))
    p_md = tb_m_desc.text_frame.paragraphs[0]
    p_md.text = "• Cluttered raw statistical numbers without context\n• Raw histograms with no patient biological baseline\n• Cryptic flags (H/L) that create anxiety without guidance"
    p_md.font.size = Pt(11)
    p_md.font.color.rgb = COLOR_BODY

    # Right: SwasthAI Report
    add_card(slide3, Inches(6.9), Inches(1.8), Inches(5.6), Inches(5.2), bg_color=RGBColor(240, 253, 250), border_color=COLOR_TEAL)
    tb_r = slide3.shapes.add_textbox(Inches(7.1), Inches(1.9), Inches(5.2), Inches(0.6))
    p_r = tb_r.text_frame.paragraphs[0]
    p_r.text = "AFTER: SwasthAI Certified Diagnostic Report"
    p_r.font.bold = True
    p_r.font.size = Pt(13)
    p_r.font.color.rgb = COLOR_DARK_TEAL

    rep_img = os.path.join(assets_dir, "swasthai_report_page_1.png")
    if os.path.exists(rep_img):
        # Embed report snapshot
        slide3.shapes.add_picture(rep_img, Inches(7.1), Inches(2.45), height=Inches(3.2))

    tb_r_desc = slide3.shapes.add_textbox(Inches(7.1), Inches(5.7), Inches(5.2), Inches(1.2))
    p_rd = tb_r_desc.text_frame.paragraphs[0]
    p_rd.text = "✓ Standardized format with Patient UHID & Referring Lab\n✓ Color-coded clinical badges (CRITICAL HIGH, NORMAL)\n✓ Digital Doctor Signature & Verifiable QR Authenticity Seal"
    p_rd.font.size = Pt(11)
    p_rd.font.color.rgb = COLOR_DARK_TEAL

    # =========================================================================
    # SLIDE 4: How SwasthAI Works (Pipeline & Architecture)
    # =========================================================================
    slide4 = prs.slides.add_slide(blank_layout)
    add_header(slide4, "System Workflow", "End-to-End Automated Diagnostic Pipeline", "From sample analysis to digital patient delivery in under 90 seconds.")

    steps = [
        ("Step 1: Patient UHID", "Register patient once or select from UHID database. Captures age, gender, and referring clinician.", "01", COLOR_BLUE),
        ("Step 2: Rapid Data Entry", "Select test panel (CBC, Lipid, LFT). Technicians input parameters in an intuitive, keyboard-optimized UI.", "02", COLOR_TEAL),
        ("Step 3: Auto-Calculation", "Calculates absolute counts, RDW ratios, and hematocrit validation automatically. Zero manual math.", "03", COLOR_GREEN),
        ("Step 4: Certified Export", "Authorized doctor digitally signs off. Generates tamper-proof PDF with live QR verification seal.", "04", COLOR_AMBER)
    ]

    for idx, (stitle, sdesc, snum, scolor) in enumerate(steps):
        x = Inches(0.8 + idx * 3.0)
        c = add_card(slide4, x, Inches(1.8), Inches(2.75), Inches(5.0), bg_color=RGBColor(255, 255, 255))
        
        # Circle Step Number
        num_circle = slide4.shapes.add_shape(MSO_SHAPE.OVAL, x + Inches(0.95), Inches(2.2), Inches(0.85), Inches(0.85))
        num_circle.fill.solid()
        num_circle.fill.fore_color.rgb = scolor
        num_circle.line.fill.background()
        p_c = num_circle.text_frame.paragraphs[0]
        p_c.text = snum
        p_c.alignment = PP_ALIGN.CENTER
        p_c.font.bold = True
        p_c.font.size = Pt(16)
        p_c.font.color.rgb = RGBColor(255, 255, 255)

        tbs = slide4.shapes.add_textbox(x + Inches(0.2), Inches(3.3), Inches(2.35), Inches(3.3))
        tfs = tbs.text_frame
        tfs.word_wrap = True
        
        p_st = tfs.paragraphs[0]
        p_st.text = stitle
        p_st.alignment = PP_ALIGN.CENTER
        p_st.font.bold = True
        p_st.font.size = Pt(14)
        p_st.font.color.rgb = COLOR_HEADING

        p_sd = tfs.add_paragraph()
        p_sd.space_before = Pt(12)
        p_sd.text = sdesc
        p_sd.alignment = PP_ALIGN.CENTER
        p_sd.font.size = Pt(11.5)
        p_sd.font.color.rgb = COLOR_BODY

    # =========================================================================
    # SLIDE 5: Who Gets Benefited & How? (Stakeholder Matrix)
    # =========================================================================
    slide5 = prs.slides.add_slide(blank_layout)
    add_header(slide5, "Stakeholder Impact", "Who Gets Benefited & How?", "Measurable value delivered across the entire healthcare diagnostic chain.")

    # 4 Quadrants
    quads = [
        ("1. Patients (The Primary User)", 
         ["• High-Contrast Color Badges: Patients easily recognize NORMAL vs CRITICAL values.",
          "• Direct WhatsApp & PDF Delivery: No waiting in crowded lab lobbies.",
          "• Unique UHID History: Maintains lifetime health records over time.",
          "• Authenticity Guarantee: QR verification prevents duplicate or fraudulent reports."],
         Inches(0.8), Inches(1.8)),
        
        ("2. Pathology Lab Technicians", 
         ["• 90% Faster Turnaround: Reduces report drafting from 15 mins to under 90 secs.",
          "• Automated Math Engine: Zero manual calculations for absolute differentials.",
          "• In-Line Error Guardrails: Flags biological impossibilities before finalization.",
          "• Keyboard-Friendly Entry: Rapid desktop or mobile field navigation."],
         Inches(6.8), Inches(1.8)),

        ("3. Doctors & Consulting Clinicians", 
         ["• Standardized Diagnostic Layout: Immediate visual triaging of abnormal parameters.",
          "• Color-Coded Alert Hierarchy: Critical high/low values prioritized for emergency care.",
          "• Clinician Digital Endorsement: Certified signature backed by timestamped logs.",
          "• Seamless EMR Integration: Easily exported or archived in hospital records."],
         Inches(0.8), Inches(4.5)),

        ("4. Laboratory Owners & Healthcare Centers", 
         ["• Low Capex Cost: Replaces expensive $5,000+ LIMS with a modern Android app.",
          "• Multi-Staff & Role Access: Admin, Pathologist, and Data Entry separation.",
          "• Cloud Synchronization: Centralized reports backed up on secure Spring Boot server.",
          "• Compliance & Branding: Custom lab logo, header, footer, and license controls."],
         Inches(6.8), Inches(4.5))
    ]

    for qtitle, points, qx, qy in quads:
        add_card(slide5, qx, qy, Inches(5.7), Inches(2.5), bg_color=RGBColor(255, 255, 255))
        tb_q = slide5.shapes.add_textbox(qx + Inches(0.25), qy + Inches(0.15), Inches(5.2), Inches(2.2))
        tfq = tb_q.text_frame
        tfq.word_wrap = True
        
        p_qt = tfq.paragraphs[0]
        p_qt.text = qtitle
        p_qt.font.bold = True
        p_qt.font.size = Pt(14)
        p_qt.font.color.rgb = COLOR_TEAL

        for pt in points:
            p_p = tfq.add_paragraph()
            p_p.space_before = Pt(4)
            p_p.text = pt
            p_p.font.size = Pt(10.5)
            p_p.font.color.rgb = COLOR_BODY

    # =========================================================================
    # SLIDE 6: Deep Dive — Patient Benefits
    # =========================================================================
    slide6 = prs.slides.add_slide(blank_layout)
    add_header(slide6, "Human-Centric Healthcare", "What Patients Gain from SwasthAI", "Empowering individuals with transparent, accessible, and anxiety-free reports.")

    # Left: Explanation list
    tb_p_left = slide6.shapes.add_textbox(Inches(0.8), Inches(1.8), Inches(6.5), Inches(5.2))
    tf_pl = tb_p_left.text_frame
    tf_pl.word_wrap = True

    items_patient = [
        ("🟢 Color-Coded Clinical Clarity", "Instead of confusing numbers like 'PLT: 23 10³/µL', SwasthAI displays a bright 'CRITICAL LOW' badge with reference intervals (150 - 450). Patients know instantly whether their results are safe or need medical attention."),
        ("⚡ Zero-Wait Digital Access", "Reports are generated digitally in PDF format and delivered instantly via WhatsApp, Email, or SMS link. Patients don't have to travel back to the lab just to collect paper."),
        ("🛡️ Tamper-Proof QR Verification", "Every report carries a cryptographically unique QR code. When scanned with any smartphone camera, it opens the verified report from the official lab server, completely preventing fake or altered medical documents."),
        ("📁 Permanent UHID Medical History", "Patients receive a unique permanent identifier (e.g. PAT-000001). All historical blood tests remain accessible for second opinions or tracking chronic illnesses over years.")
    ]

    for idx, (ititle, idesc) in enumerate(items_patient):
        p_it = tf_pl.paragraphs[0] if idx == 0 else tf_pl.add_paragraph()
        p_it.space_before = Pt(10) if idx > 0 else Pt(0)
        p_it.text = ititle
        p_it.font.bold = True
        p_it.font.size = Pt(13.5)
        p_it.font.color.rgb = COLOR_HEADING

        p_id = tf_pl.add_paragraph()
        p_id.space_before = Pt(2)
        p_id.text = idesc
        p_id.font.size = Pt(11)
        p_id.font.color.rgb = COLOR_BODY

    # Right: Snippet of Report Page 2 showing QR code & signature
    add_card(slide6, Inches(7.7), Inches(1.8), Inches(4.8), Inches(5.2), bg_color=RGBColor(255, 255, 255), border_color=COLOR_TEAL)
    rep_img2 = os.path.join(assets_dir, "swasthai_report_page_2.png")
    if os.path.exists(rep_img2):
        slide6.shapes.add_picture(rep_img2, Inches(7.9), Inches(2.0), width=Inches(4.4))

    # =========================================================================
    # SLIDE 7: App Screen Showcase (The User Experience)
    # =========================================================================
    slide7 = prs.slides.add_slide(blank_layout)
    add_header(slide7, "Mobile Interface", "Inside the SwasthAI Android App", "Built with modern Jetpack Compose for high-speed clinical workflow.")

    screens = [
        ("Dashboard & Worklist", "Real-time metrics, active draft reports, daily quota usage, and rapid action shortcuts.", "app_dashboard.jpg"),
        ("Patient Selection & UHID", "Instant search by UHID, phone, or name. Visual confirmation badge with age and clinical metadata.", "app_patient_select.jpg"),
        ("Clinical Workspace", "Test panel selection, in-line calculations, CBC parameter validation, and clinician sign-off.", "app_workspace.jpg")
    ]

    for idx, (stitle, sdesc, sfile) in enumerate(screens):
        x = Inches(0.8 + idx * 4.0)
        add_card(slide7, x, Inches(1.8), Inches(3.7), Inches(5.2), bg_color=RGBColor(255, 255, 255))
        
        tb_sc = slide7.shapes.add_textbox(x + Inches(0.2), Inches(1.9), Inches(3.3), Inches(0.9))
        tf_sc = tb_sc.text_frame
        tf_sc.word_wrap = True
        p_sct = tf_sc.paragraphs[0]
        p_sct.text = stitle
        p_sct.font.bold = True
        p_sct.font.size = Pt(13)
        p_sct.font.color.rgb = COLOR_TEAL

        p_scd = tf_sc.add_paragraph()
        p_scd.text = sdesc
        p_scd.font.size = Pt(10)
        p_scd.font.color.rgb = COLOR_BODY

        img_path = os.path.join(assets_dir, sfile)
        if os.path.exists(img_path):
            slide7.shapes.add_picture(img_path, x + Inches(0.4), Inches(2.9), width=Inches(2.9))

    # =========================================================================
    # SLIDE 8: Technical Architecture (Classroom Defense)
    # =========================================================================
    slide8 = prs.slides.add_slide(blank_layout)
    add_header(slide8, "Engineering & Architecture", "Technical Stack & System Design", "How SwasthAI is engineered for reliability, zero crashes, and enterprise scale.")

    tech_cards = [
        ("Android Client Stack", [
            "• Language & Framework: 100% Kotlin with modern Jetpack Compose (Declarative UI)",
            "• Design System: Material 3 with dynamic theming and high-contrast accessibility",
            "• Architecture: Clean Architecture + MVVM + StateFlow + Coroutines",
            "• Networking & Security: Retrofit, OkHttp, JWT TokenAuthenticator with single-flight refresh",
            "• Hardened: Bulletproof try-catch shields on PDF FileProviders, Intents, and OutOfMemoryError handlers"
        ], Inches(0.8), Inches(1.8)),
        
        ("Backend & Cloud Infrastructure", [
            "• Framework: Java Spring Boot with Spring Security & JPA",
            "• Database: High-availability PostgreSQL with ACID compliance",
            "• PDF Generation Engine: Server-side iText vector engine with embedded QR code generator",
            "• Multi-Tenancy: Role-based access control (Admin, Clinician, Lab Staff)",
            "• Production Cloud: Deployed with automated health monitors and SSL/TLS encryption"
        ], Inches(6.8), Inches(1.8)),

        ("Security & Integrity Safeguards", [
            "• Cryptographic QR Verification: Direct URL linking to live cloud backend record",
            "• Authorized Digital Signatures: Base64 canvas captured pathologist signature seals",
            "• Data Isolation: Laboratory-level data segregation protecting patient confidentiality",
            "• Offline Resiliency: Cache management and graceful network retry mechanisms"
        ], Inches(0.8), Inches(4.5)),

        ("Key Performance Metrics", [
            "• Turnaround Time: Decreased from 15 minutes to < 90 seconds (90% faster)",
            "• Human Calculation Errors: Reduced to 0% with auto-differential algorithms",
            "• App Footprint: Under 17 MB standalone APK / AAB package size",
            "• Compatibility: Android 8.0 (API 26) up to Android 15 (API 35)"
        ], Inches(6.8), Inches(4.5))
    ]

    for title, points, cx, cy in tech_cards:
        add_card(slide8, cx, cy, Inches(5.7), Inches(2.5), bg_color=RGBColor(255, 255, 255))
        tb_t = slide8.shapes.add_textbox(cx + Inches(0.25), cy + Inches(0.15), Inches(5.2), Inches(2.2))
        tft = tb_t.text_frame
        tft.word_wrap = True
        
        p_tt = tft.paragraphs[0]
        p_tt.text = title
        p_tt.font.bold = True
        p_tt.font.size = Pt(13)
        p_tt.font.color.rgb = COLOR_HEADING

        for pt in points:
            p_pt = tft.add_paragraph()
            p_pt.space_before = Pt(3)
            p_pt.text = pt
            p_pt.font.size = Pt(10)
            p_pt.font.color.rgb = COLOR_BODY

    # =========================================================================
    # SLIDE 9: Project Conclusion & Future Roadmap
    # =========================================================================
    slide9 = prs.slides.add_slide(blank_layout)
    add_header(slide9, "Conclusion & Future", "Transforming Pathology for the Next Decade", "Summary of project achievements and future technological horizons.")

    # 3 Summary Cards
    summary_cols = [
        ("What We Delivered", 
         "A full-fledged, publication-ready Android diagnostic management system that solves the manual bottlenecks of clinical laboratories and makes diagnostic reports instantly readable and verifiable for patients.",
         COLOR_TEAL),
        ("Social & Clinical Impact", 
         "Brings enterprise-grade LIMS capabilities to rural and suburban diagnostic clinics at a fraction of the cost, while protecting patients from diagnostic transcription errors and fake medical reports.",
         COLOR_GREEN),
        ("Future Roadmap", 
         "• Computer Vision (OCR) to photograph analyzer screens and auto-populate values\n• AI-assisted diagnostic commentary & trend analysis\n• Direct WhatsApp Cloud API and ABHA (Ayushman Bharat Digital Health) integration",
         COLOR_BLUE)
    ]

    for idx, (stitle, sdesc, scolor) in enumerate(summary_cols):
        x = Inches(0.8 + idx * 4.0)
        add_card(slide9, x, Inches(1.8), Inches(3.7), Inches(4.8), bg_color=RGBColor(255, 255, 255), border_color=BORDER_CARD)
        
        badge_box = slide9.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x + Inches(0.3), Inches(2.1), Inches(3.1), Inches(0.4))
        badge_box.fill.solid()
        badge_box.fill.fore_color.rgb = RGBColor(240, 253, 250)
        badge_box.line.color.rgb = scolor
        p_b = badge_box.text_frame.paragraphs[0]
        p_b.text = stitle
        p_b.alignment = PP_ALIGN.CENTER
        p_b.font.bold = True
        p_b.font.size = Pt(11)
        p_b.font.color.rgb = scolor

        tb_sc = slide9.shapes.add_textbox(x + Inches(0.3), Inches(2.7), Inches(3.1), Inches(3.7))
        tfs = tb_sc.text_frame
        tfs.word_wrap = True
        p_sd = tfs.paragraphs[0]
        p_sd.text = sdesc
        p_sd.font.size = Pt(12)
        p_sd.font.color.rgb = COLOR_BODY

    # Save presentation
    output_path = r"d:/Java-spring-boot-projects/swasthai-report-generator/SwasthAI_Report_Generator_Presentation.pptx"
    prs.save(output_path)
    print("PowerPoint presentation generated successfully at:", output_path)

if __name__ == "__main__":
    create_presentation()

