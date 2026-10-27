package com.engrshuvo.financemanager

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.OutputStream

@Implements(PdfDocument::class)
class ShadowPdfDocument {
    private val pages = mutableListOf<PdfDocument.Page>()
    private var isClosed = false

    @Implementation
    fun __constructor__() {
        // Prevent native constructor from creating native pointers in JVM
    }

    @Implementation
    fun startPage(pageInfo: PdfDocument.PageInfo): PdfDocument.Page {
        if (isClosed) {
            throw IllegalStateException("document is closed!")
        }
        val constructor = PdfDocument.Page::class.java.declaredConstructors.firstOrNull { it.parameterTypes.size == 2 }
        val bitmap = Bitmap.createBitmap(pageInfo.pageWidth.coerceAtLeast(1), pageInfo.pageHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val page = if (constructor != null) {
            constructor.isAccessible = true
            constructor.newInstance(canvas, pageInfo) as PdfDocument.Page
        } else {
            val fallbackConstructor = PdfDocument.Page::class.java.declaredConstructors.first()
            fallbackConstructor.isAccessible = true
            if (fallbackConstructor.parameterTypes.isEmpty()) {
                val p = fallbackConstructor.newInstance() as PdfDocument.Page
                try {
                    val cField = PdfDocument.Page::class.java.getDeclaredField("mCanvas").apply { isAccessible = true }
                    cField.set(p, canvas)
                } catch (_: Exception) {}
                try {
                    val infoField = PdfDocument.Page::class.java.getDeclaredField("mPageInfo").apply { isAccessible = true }
                    infoField.set(p, pageInfo)
                } catch (_: Exception) {}
                p
            } else {
                fallbackConstructor.newInstance(canvas, pageInfo) as PdfDocument.Page
            }
        }
        pages.add(page)
        return page
    }

    @Implementation
    fun finishPage(page: PdfDocument.Page) {
        if (isClosed) {
            throw IllegalStateException("document is closed!")
        }
    }

    @Implementation
    fun writeTo(out: OutputStream) {
        if (isClosed) {
            throw IllegalStateException("document is closed!")
        }
        out.write("%PDF-1.4\n%DailyBudgetAndLoanManager\n%%EOF\n".toByteArray(Charsets.US_ASCII))
        out.flush()
    }

    @Implementation
    fun close() {
        isClosed = true
        pages.clear()
    }
}
