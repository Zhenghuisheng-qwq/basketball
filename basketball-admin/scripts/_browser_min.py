from playwright.sync_api import sync_playwright
with sync_playwright() as p:
    b=p.chromium.launch(headless=True)
    page=b.new_page()
    r=page.goto("http://127.0.0.1:5173/", wait_until="domcontentloaded", timeout=45000)
    print("status", r.status if r else None)
    page.wait_for_timeout(1500)
    try:
        t=page.locator("body").inner_text(timeout=10000)
        print("inner_text type", type(t), t[:80].replace("\n"," "))
    except Exception as e:
        print("inner_text err", type(e), e)
    try:
        t2=page.inner_text("body")
        print("page.inner_text", t2[:80].replace("\n"," "))
    except Exception as e:
        print("page.inner_text err", e)
    b.close()