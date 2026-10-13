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
        val constructor = PdfDocument.Page::class.java.declaredConstructors.first()
        constructor.isAccessible = true
        val bitmap = Bitmap.createBitmap(pageInfo.pageWidth.coerceAtLeast(1), pageInfo.pageHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val page = constructor.newInstance(canvas, pageInfo) as PdfDocument.Page
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
