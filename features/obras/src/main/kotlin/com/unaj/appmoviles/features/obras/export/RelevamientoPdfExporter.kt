package com.unaj.appmoviles.features.obras.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import java.io.File
import java.io.FileOutputStream

object RelevamientoPdfExporter {

    @Suppress("DEPRECATION")
    fun exportRelevamientoToPdf(context: Context, relevamiento: Relevamiento, obra: Obra?): Uri? {
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = PdfDocument()

            // A4 format: 595 x 842 points
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var currentPage = pdfDocument.startPage(pageInfo)
            var canvas = currentPage.canvas

            val petroleumColor = Color.parseColor("#172331")
            val orangeColor = Color.parseColor("#F28A24")

            val titlePaint = TextPaint().apply {
                color = petroleumColor
                textSize = 28f
                isFakeBoldText = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val subtitlePaint = TextPaint().apply {
                color = petroleumColor
                textSize = 16f
                isFakeBoldText = true
            }

            val tableHeaderPaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 14f
                isFakeBoldText = true
            }

            val tableRowPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 14f
            }

            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            val headerBgPaint = Paint().apply {
                color = petroleumColor
                style = Paint.Style.FILL
            }

            val padding = 40f
            val contentWidth = pageInfo.pageWidth - (padding * 2).toInt()
            var currentY = padding

            // --- HEADER ---
            // Try to load app logo if possible
            val logoId = context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)
            if (logoId != 0) {
                val bitmap = BitmapFactory.decodeResource(context.resources, logoId)
                if (bitmap != null) {
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true)
                    canvas.drawBitmap(scaledBitmap, padding, currentY, null)
                }
            }

            // Company/App Name next to logo (or just title)
            val brandPaint = TextPaint().apply {
                color = orangeColor
                textSize = 20f
                isFakeBoldText = true
            }
            canvas.drawText("ObraTrack", padding + 60f, currentY + 32f, brandPaint)
            
            currentY += 80f

            // Title
            canvas.drawText("Reporte de Relevamiento", padding, currentY, titlePaint)
            currentY += 40f

            // --- TABLE ---
            val tableTop = currentY
            val rowHeight = 30f
            val column1Width = contentWidth * 0.45f

            // Draw Header
            canvas.drawRect(padding, currentY, padding + contentWidth, currentY + rowHeight, headerBgPaint)
            canvas.drawText("Propiedad", padding + 10f, currentY + 20f, tableHeaderPaint)
            canvas.drawText("Detalle", padding + column1Width + 10f, currentY + 20f, tableHeaderPaint)
            currentY += rowHeight

            // Helper to draw rows
            fun drawTableRow(label: String, value: String) {
                canvas.drawRect(padding, currentY, padding + contentWidth, currentY + rowHeight, Paint().apply { 
                    color = Color.WHITE; style = Paint.Style.FILL 
                })
                canvas.drawText(label, padding + 10f, currentY + 20f, subtitlePaint)
                canvas.drawText(value, padding + column1Width + 10f, currentY + 20f, tableRowPaint)
                
                // borders
                canvas.drawLine(padding, currentY, padding, currentY + rowHeight, linePaint)
                canvas.drawLine(padding + column1Width, currentY, padding + column1Width, currentY + rowHeight, linePaint)
                canvas.drawLine(padding + contentWidth, currentY, padding + contentWidth, currentY + rowHeight, linePaint)
                canvas.drawLine(padding, currentY + rowHeight, padding + contentWidth, currentY + rowHeight, linePaint)
                
                currentY += rowHeight
            }

            // Populate Table
            drawTableRow("Obra", obra?.nombre ?: "Sin asignar")
            drawTableRow("Cliente", obra?.cliente ?: "Desconocido")
            
            val fechaInicioObra = obra?.fechaInicio?.let { formatFecha(it) } ?: "N/A"
            drawTableRow("Fecha de Inicio", fechaInicioObra)
            
            val fechaRelevamiento = formatFecha(relevamiento.fecha)
            drawTableRow("Fecha de Relevamiento", fechaRelevamiento)

            // Outer table border
            canvas.drawLine(padding, tableTop, padding + contentWidth, tableTop, linePaint)

            currentY += 40f

            // --- NOTES ---
            canvas.drawText("Notas / Descripción del relevamiento:", padding, currentY, subtitlePaint)
            currentY += 25f

            val textLayout = StaticLayout(
                relevamiento.nota.ifBlank { "Sin notas registradas." },
                tableRowPaint,
                contentWidth,
                Layout.Alignment.ALIGN_NORMAL,
                1.1f,
                0f,
                false
            )

            // Draw notes box background
            val notesBgPaint = Paint().apply {
                color = Color.parseColor("#F8F9FA")
                style = Paint.Style.FILL
            }

            // Pagination Logic
            var layoutY = 0
            
            // Footer height logic
            val footerHeight = 60f
            var heightRemaining = pageInfo.pageHeight - currentY - padding - footerHeight

            fun drawFooter(c: Canvas) {
                val footerPaint = TextPaint().apply {
                    color = Color.GRAY
                    textSize = 12f
                    textAlign = Paint.Align.CENTER
                }
                c.drawText(
                    "Generado por ObraTrack - Obras reales, en un solo lugar.", 
                    pageInfo.pageWidth / 2f, 
                    pageInfo.pageHeight - padding, 
                    footerPaint
                )
            }

            while (layoutY < textLayout.height) {
                if (heightRemaining <= 0) {
                    drawFooter(canvas)
                    pdfDocument.finishPage(currentPage)
                    currentPage = pdfDocument.startPage(pageInfo)
                    canvas = currentPage.canvas
                    currentY = padding
                    heightRemaining = pageInfo.pageHeight - padding * 2 - footerHeight
                }

                val clipBottom = minOf(layoutY + heightRemaining.toInt(), textLayout.height)
                val drawnHeight = clipBottom - layoutY
                
                // Draw background for this part of the notes
                canvas.drawRect(padding, currentY, padding + contentWidth, currentY + drawnHeight + 20f, notesBgPaint)
                canvas.drawRect(padding, currentY, padding + contentWidth, currentY + drawnHeight + 20f, linePaint.apply { style = Paint.Style.STROKE })

                canvas.save()
                canvas.translate(padding + 10f, currentY + 10f - layoutY)
                canvas.clipRect(0, layoutY, contentWidth - 20, clipBottom)
                textLayout.draw(canvas)
                canvas.restore()

                currentY += drawnHeight + 20f
                layoutY = clipBottom
                heightRemaining -= (drawnHeight + 20f)
            }

            drawFooter(canvas)
            pdfDocument.finishPage(currentPage)

            // Save the document to cache
            val file = File(context.cacheDir, "relevamiento_${obra?.nombre?.replace(" ", "_") ?: relevamiento.id}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))

            // Obtain URI using FileProvider
            val authority = "${context.packageName}.fileprovider"
            return FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument?.close()
        }
    }
}