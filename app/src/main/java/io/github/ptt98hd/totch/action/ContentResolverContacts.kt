package io.github.ptt98hd.totch.action

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract.Contacts
import io.github.ptt98hd.totch.ContactEntry
import io.github.ptt98hd.totch.ContactSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Contacts for the picker (actions spec §17); empty without `READ_CONTACTS`. */
class ContentResolverContacts(private val context: Context) : ContactSource {
    override suspend fun contacts(): List<ContactEntry> = withContext(Dispatchers.IO) {
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext emptyList()
        }
        val projection = arrayOf(Contacts._ID, Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME_PRIMARY)
        context.contentResolver.query(Contacts.CONTENT_URI, projection, null, null, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(2)?.takeIf { it.isNotBlank() } ?: continue
                    val key = cursor.getString(1) ?: continue
                    add(ContactEntry(Contacts.getLookupUri(cursor.getLong(0), key).toString(), name))
                }
            }
        }.orEmpty()
    }
}
