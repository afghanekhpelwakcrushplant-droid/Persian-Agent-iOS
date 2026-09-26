const $ = id => document.getElementById(id);
const command = $("command"), result = $("result"), status = $("status");

function localPlan(text){
  const t = text.trim();
  if(!t) return "دستور خالی است.";
  if(/یادآوری|یادم|یادآور/.test(t))
    return "دستور یادآوری شناسایی شد. در نسخه بعدی، این بخش به موتور Task/Reminder متصل می‌شود.";
  if(/جستجو|سرچ|بررسی کن|تحقیق/.test(t))
    return "درخواست تحقیق شناسایی شد. موتور Web Research در مرحله بعد به این بخش متصل می‌شود.";
  if(/فایل|pdf|پی‌دی‌اف|عکس|تصویر/.test(t))
    return "درخواست فایل شناسایی شد. ابزارهای File/PDF/OCR در مرحله بعد اضافه می‌شوند.";
  return "دستور دریافت شد. موتور Agent در حال آماده‌سازی است.";
}

$("run").addEventListener("click",()=>{
  const text = command.value;
  status.textContent = "در حال پردازش...";
  result.textContent = localPlan(text);
  status.textContent = "آماده.";
});

$("speak").addEventListener("click",()=>{
  const text = result.textContent;
  if("speechSynthesis" in window){
    speechSynthesis.cancel();
    const u = new SpeechSynthesisUtterance(text);
    u.lang = "fa-IR";
    speechSynthesis.speak(u);
  } else status.textContent = "خواندن صوتی در این مرورگر در دسترس نیست.";
});

if("serviceWorker" in navigator) navigator.serviceWorker.register("sw.js").catch(()=>{});
