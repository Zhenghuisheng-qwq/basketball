import html
import re
import smtplib
import ssl
import time
from email.message import EmailMessage
from email.utils import formataddr
from pathlib import Path

import pymysql

NOTICE_ID = 2
INTERVAL_SEC = 60
ROOT = Path(__file__).resolve().parent


def mask_email(email):
    if not email or "@" not in email:
        return "***"
    local, domain = email.split("@", 1)
    visible = local[:1] + "*" if len(local) <= 2 else local[:2] + "***"
    return visible + "@" + domain


def load_mail():
    text = (ROOT / "basketball-admin/src/main/resources/application.yml").read_text(encoding="utf-8")
    mail = text.split("mail:", 1)[1]
    host = re.search(r"host:\s*(\S+)", mail).group(1)
    port = int(re.search(r"port:\s*(\d+)", mail).group(1))
    username = re.search(r"username:\s*(\S+)", mail).group(1)
    password = re.search(r"password:\s*(\S+)", mail).group(1).strip("'").strip('"')
    from_match = re.search(r"from:\s*(\S+)", text)
    from_name = re.search(r"from-name:\s*(\S+)", text)
    sender = from_match.group(1) if from_match else username
    name = from_name.group(1) if from_name else "CNPA"
    return host, port, username, password, sender, name


def db():
    text = (ROOT / "basketball-admin/src/main/resources/application-druid.yml").read_text(encoding="utf-8")
    url = re.search(r"jdbc:mysql://([^:/]+):(\d+)/(\w+)", text)
    user = re.search(r"username:\s*(\S+)", text)
    password = re.search(r"password:\s*(\S+)", text)
    return pymysql.connect(
        host=url.group(1),
        port=int(url.group(2)),
        user=user.group(1),
        password=password.group(1).strip("'").strip('"'),
        database=url.group(3),
        charset="utf8mb4",
        connect_timeout=15,
        read_timeout=30,
        autocommit=True,
    )


def build_html(subject, content):
    body = (content or "").replace("<img", '<img style="max-width:100%;height:auto;"')
    safe_subject = html.escape(subject or "")
    return (
        '<!DOCTYPE html><html><head><meta charset="UTF-8"></head>'
        '<body style="margin:0;padding:24px;font-family:Arial,Helvetica,sans-serif;color:#222;line-height:1.7;">'
        f'<h2 style="margin:0 0 16px;font-size:20px;">{safe_subject}</h2>'
        f"{body}</body></html>"
    )


def send_one(host, port, username, password, sender, sender_name, to_email, subject, content):
    message = EmailMessage()
    message["From"] = formataddr((sender_name, sender))
    message["To"] = to_email
    message["Subject"] = subject
    message.set_content("请使用支持 HTML 的邮箱查看这封通知。")
    message.add_alternative(build_html(subject, content), subtype="html")
    client = smtplib.SMTP_SSL(host, port, timeout=30, context=ssl.create_default_context())
    try:
        client.login(username, password)
        client.send_message(message)
    finally:
        try:
            client.quit()
        except Exception:
            pass


def main():
    host, port, username, password, sender, sender_name = load_mail()
    conn = db()
    cur = conn.cursor()
    cur.execute("SELECT title, content_html, send_status FROM basket_mail_notice WHERE id=%s AND is_delete=0", (NOTICE_ID,))
    notice = cur.fetchone()
    if not notice:
        print("NO_NOTICE")
        return
    if notice[2] == 1:
        print("ABORT_ALREADY_SENDING")
        return
    subject = (notice[0] or "CNPA通知").replace("\r", " ").replace("\n", " ").strip()
    content = notice[1] or ""
    cur.execute(
        """
        SELECT id, email FROM basket_mail_notice_recipient
        WHERE notice_id=%s AND send_status=2
        ORDER BY id
        """,
        (NOTICE_ID,),
    )
    pending = cur.fetchall()
    print(f"START failed={len(pending)} interval={INTERVAL_SEC}s", flush=True)
    sent = 0
    failed = 0
    for index, (recipient_id, email) in enumerate(pending):
        started = time.monotonic()
        address = (email or "").strip()
        status = 2
        reason = None
        try:
            if "@" not in address:
                reason = "邮箱格式不正确"
            else:
                send_one(host, port, username, password, sender, sender_name, address, subject, content)
                status = 1
                sent += 1
        except smtplib.SMTPAuthenticationError as ex:
            detail = ex.smtp_error.decode("utf-8", "replace") if isinstance(ex.smtp_error, bytes) else str(ex.smtp_error)
            reason = f"SMTP {ex.smtp_code} {detail}"[:240]
            failed += 1
            cur.execute(
                """
                UPDATE basket_mail_notice_recipient
                SET send_status=2, fail_reason=%s, send_time=NOW()
                WHERE id=%s AND notice_id=%s AND send_status=2
                """,
                (reason, recipient_id, NOTICE_ID),
            )
            print(f"AUTH_STOP to={mask_email(address)} {reason}", flush=True)
            break
        except Exception as ex:
            reason = f"{type(ex).__name__}: {ex}"[:240]
            failed += 1
            lowered = reason.lower()
            if "login fail" in lowered or "authentication" in lowered or "disconnected" in lowered:
                cur.execute(
                    """
                    UPDATE basket_mail_notice_recipient
                    SET send_status=2, fail_reason=%s, send_time=NOW()
                    WHERE id=%s AND notice_id=%s AND send_status=2
                    """,
                    (reason, recipient_id, NOTICE_ID),
                )
                print(f"AUTH_STOP to={mask_email(address)} {reason}", flush=True)
                break
        if status == 1:
            cur.execute(
                """
                UPDATE basket_mail_notice_recipient
                SET send_status=1, fail_reason=NULL, send_time=NOW()
                WHERE id=%s AND notice_id=%s AND send_status=2
                """,
                (recipient_id, NOTICE_ID),
            )
            print(f"OK {index + 1}/{len(pending)} to={mask_email(address)}", flush=True)
        elif reason and "SMTP" not in (reason or ""):
            cur.execute(
                """
                UPDATE basket_mail_notice_recipient
                SET send_status=2, fail_reason=%s, send_time=NOW()
                WHERE id=%s AND notice_id=%s AND send_status=2
                """,
                (reason, recipient_id, NOTICE_ID),
            )
            print(f"FAIL {index + 1}/{len(pending)} to={mask_email(address)} {reason}", flush=True)
        cur.execute(
            """
            UPDATE basket_mail_notice n
            SET success_count=(SELECT COUNT(*) FROM basket_mail_notice_recipient r WHERE r.notice_id=n.id AND r.send_status=1),
                fail_count=(SELECT COUNT(*) FROM basket_mail_notice_recipient r WHERE r.notice_id=n.id AND r.send_status=2),
                update_time=NOW()
            WHERE n.id=%s
            """,
            (NOTICE_ID,),
        )
        if index == len(pending) - 1 or (reason and reason.startswith("SMTP")):
            break
        wait = INTERVAL_SEC - (time.monotonic() - started)
        if wait > 0:
            time.sleep(wait)
    print(f"DONE ok={sent} failed_this_run={failed}", flush=True)
    conn.close()


if __name__ == "__main__":
    main()
