package com.example.privatecheck.logic

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailService {

    private const val TAG = "EmailService"

    suspend fun sendEmail(
        toEmail: String,
        subject: String,
        body: String,
        senderEmail: String,
        senderPassword: String,
        smtpHost: String = "smtp.gmail.com",
        smtpPort: String = "587"
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", smtpHost)
                    put("mail.smtp.port", smtpPort)
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(senderEmail, senderPassword)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(senderEmail))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                    setSubject(subject)
                    setText(body)
                }

                Transport.send(message)
                Log.d(TAG, "Email sent successfully to $toEmail")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send email", e)
                e.printStackTrace()
                false
            }
        }
    }
}
