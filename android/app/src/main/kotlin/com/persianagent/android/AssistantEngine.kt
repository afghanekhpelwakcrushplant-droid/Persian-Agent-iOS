package com.persianagent.android

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AssistantEngine(private val context: Context) {
    private var pendingCall: (() -> Unit)? = null

    fun handle(command: String): String {
        val n = normalize(command)
        if (isConfirmation(n)) {
            val action = pendingCall ?: return "عملی برای تأیید ندارم."
            pendingCall = null
            action.invoke()
            return "انجام شد."
        }
        if (isCancellation(n)) {
            pendingCall = null
            return "لغو شد."
        }
        return when {
            n.contains("تماس") || n.contains("زنگ") -> prepareCall(n)
            n.contains("پیام") || n.contains("اس ام اس") || n.contains("اس‌ام‌اس") -> prepareSms(n)
            n.contains("یادآور") || n.contains("یادم بنداز") || n.contains("یادآوری") -> createReminder(n)
            n.startsWith("جستجو") || n.startsWith("سرچ") || n.startsWith("بگرد") -> searchWeb(n)
            n.contains("ساعت") && (n.contains("چنده") || n.contains("الان")) -> currentTime()
            else -> "فرمان را فهمیدم، ولی ابزار اجرای آن هنوز به این نسخه اضافه نشده است."
        }
    }

    private fun prepareCall(n: String): String {
        val number = extractPhone(n)
        if (number != null) {
            pendingCall = { makeCall(number) }
            return "آماده تماس با $number هستم. برای تأیید بگو «تأیید می‌کنم»."
        }
        val name = n.replace("تماس", "").replace("زنگ", "")
            .replace("بگیر", "").replace("بزن", "").replace("با", "").trim()
        val contactNumber = findContactNumber(name)
            ?: return "مخاطب را پیدا نکردم یا مجوز مخاطبین داده نشده است."
        pendingCall = { makeCall(contactNumber) }
        return "برای تماس با $name آماده‌ام. برای تأیید بگو «تأیید می‌کنم»."
    }

    private fun makeCall(number: String) {
        if (context.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) return
        context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun prepareSms(n: String): String {
        val number = extractPhone(n) ?: return "برای پیام در این نسخه، شماره تلفن را داخل فرمان بگو."
        val message = n.substringAfter("پیام", "").trim()
            .replaceFirst(Regex("""^(بفرست|ارسال کن)\s*"""), "")
        context.startActivity(Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$number")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        return "صفحه پیام برای $number باز شد."
    }

    private fun createReminder(n: String): String {
        val m = Regex("""(?:در\s*)?(\d+)\s*(دقیقه|ساعت)""").find(n)
            ?: return "زمان یادآوری را مثلاً «در ۱۰ دقیقه» یا «در ۲ ساعت» بگو."
        val value = m.groupValues[1].toLong()
        val minutes = if (m.groupValues[2] == "ساعت") value * 60 else value
        val title = n.replace(m.value, "")
            .replace("یادم بنداز", "").replace("یادآوری", "").replace("یادآور", "")
            .trim().ifBlank { "یادآوری" }

        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).putExtra("text", title)
        val pending = PendingIntent.getBroadcast(
            context, (System.currentTimeMillis() and 0x7fffffff).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + minutes * 60_000L, pending)
        return "یادآور «$title» برای $minutes دقیقه دیگر ثبت شد."
    }

    private fun searchWeb(n: String): String {
        val q = n.replaceFirst(Regex("""^(جستجو|سرچ|بگرد)\s*"""), "").trim()
        if (q.isBlank()) return "موضوع جستجو را بگو."
        val url = "https://www.google.com/search?q=" + Uri.encode(q)
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "جستجوی «$q» را باز کردم."
    }

    private fun currentTime(): String =
        SimpleDateFormat("HH:mm", Locale("fa", "IR")).format(Date()).let { "الان ساعت $it است." }

    private fun extractPhone(n: String): String? =
        Regex("""\+?\d[\d\s-]{7,}""").find(n)?.value?.replace(Regex("""[\s-]"""), "")

    private fun findContactNumber(name: String): String? {
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val display = c.getString(0) ?: continue
                if (display.contains(name, ignoreCase = true)) return c.getString(1)
            }
        }
        return null
    }

    private fun normalize(value: String): String =
        value.trim().replace('ي', 'ی').replace('ك', 'ک')
            .replace(Regex("[۰-۹]")) { "۰۱۲۳۴۵۶۷۸۹".indexOf(it.value[0]).toString() }
            .replace(Regex("[٠-٩]")) { "٠١٢٣٤٥٦٧٨٩".indexOf(it.value[0]).toString() }

    private fun isConfirmation(n: String): Boolean =
        n == "تأیید می‌کنم" || n == "تایید می‌کنم" || n == "تأیید" || n == "تایید" || n == "بله"

    private fun isCancellation(n: String): Boolean =
        n == "لغو" || n == "نه" || n == "خیر" || n == "انصراف"
}
