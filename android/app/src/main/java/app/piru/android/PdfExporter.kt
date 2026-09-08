package app.piru.android

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

object PdfExporter {
    fun writeJournal(entries: List<DoseEntry>, output: OutputStream) {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(612, 792, 1).create())
        val paint = Paint().apply { textSize = 12f }
        page.canvas.drawText("Piru journal", 40f, 40f, paint)
        entries.take(45).forEachIndexed { index, dose ->
            page.canvas.drawText(
                "${dose.substance} — ${dose.amount} ${dose.unit} (${dose.route})",
                40f, 65f + index * 15f, paint
            )
        }
        document.finishPage(page)
        document.writeTo(output)
        document.close()
    }
}
