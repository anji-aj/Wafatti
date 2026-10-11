package com.aj.wafatti.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.aj.wafatti.core.utils.PhoneNumberNormalizer
import com.aj.wafatti.domain.model.DeviceContact
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceContactsReader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun readContacts(): List<DeviceContact> {
        val contactsMap = mutableMapOf<String, DeviceContact>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"
            )

            cursor?.use { c ->
                val idIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                while (c.moveToNext()) {
                    val contactId = if (idIndex >= 0) c.getString(idIndex) ?: "" else ""
                    val name = if (nameIndex >= 0) c.getString(nameIndex) ?: "Unknown" else "Unknown"
                    val number = if (numberIndex >= 0) c.getString(numberIndex) ?: "" else ""
                    val photoUri = if (photoIndex >= 0) c.getString(photoIndex) else null

                    val normalized = PhoneNumberNormalizer.normalize(number)
                    if (normalized.isNotBlank() && !contactsMap.containsKey(normalized)) {
                        contactsMap[normalized] = DeviceContact(
                            id = contactId,
                            name = name,
                            phoneNumber = number,
                            normalizedPhone = normalized,
                            photoUri = photoUri
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            // SecurityException if permission not granted
            return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }

        return contactsMap.values.toList()
    }
}
