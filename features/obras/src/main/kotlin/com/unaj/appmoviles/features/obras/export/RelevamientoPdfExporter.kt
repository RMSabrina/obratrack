package com.unaj.appmoviles.features.obras.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.ResumenTareas
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RelevamientoPdfExporter {

    private val primaryNavy = Color.parseColor("#1B365D")
    private val accentAmber = Color.parseColor("#E65100")
    private val surfaceGray = Color.parseColor("#F8F9FA")
    private val borderGray = Color.parseColor("#D0D7DE")
    private val textDark = Color.parseColor("#1F2937")
    private val textMuted = Color.parseColor("#6B7280")
    private val successGreen = Color.parseColor("#166534")
    private val alertRed = Color.parseColor("#991B1B")

    private val chipBgPending = Color.parseColor("#F3F4F6")
    private val chipTextPending = Color.parseColor("#4B5563")
    private val chipBgEnCurso = Color.parseColor("#DBEAFE")
    private val chipTextEnCurso = Color.parseColor("#1E40AF")
    private val chipBgCompletada = Color.parseColor("#DCFCE7")
    private val chipTextCompletada = Color.parseColor("#166534")

    // Formato estándar A4: 595 x 842 puntos
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val PADDING = 40f

    @Suppress("DEPRECATION")
    fun exportRelevamientoToPdf(
        context: Context,
        relevamiento: Relevamiento,
        obra: Obra?,
        tareaTitulo: String? = null
    ): Uri? {
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            var currentPage = pdfDocument.startPage(pageInfo)
            var canvas = currentPage.canvas

            val contentWidth = (PAGE_WIDTH - (PADDING * 2)).toInt()
            var currentY = PADDING

            // 1. HEADER
            currentY = drawHeader(
                context = context,
                canvas = canvas,
                title = "ACTA TÉCNICA DE RELEVAMIENTO",
                subtitle = "Supervisión y Auditoría Técnica de Obra",
                startY = currentY,
                contentWidth = contentWidth
            )

            // 2. TABLA DE METADATOS DE VISITA
            currentY = drawRelevamientoMetaTable(
                canvas = canvas,
                obra = obra,
                relevamiento = relevamiento,
                tareaTitulo = tareaTitulo,
                startY = currentY,
                contentWidth = contentWidth
            )

            currentY += 24f

            // 3. DICTAMEN TÉCNICO Y OBSERVACIONES
            val titlePaint = TextPaint().apply {
                color = primaryNavy
                textSize = 14f
                isFakeBoldText = true
            }
            canvas.drawText("DICTAMEN TÉCNICO Y OBSERVACIONES DE CAMPO", PADDING, currentY, titlePaint)
            currentY += 14f

            val notePaint = TextPaint().apply {
                color = textDark
                textSize = 11.5f
            }
            val textLayout = StaticLayout(
                relevamiento.nota.ifBlank { "Sin observaciones registradas durante esta visita técnica." },
                notePaint,
                contentWidth - 24,
                Layout.Alignment.ALIGN_NORMAL,
                1.2f,
                0f,
                false
            )

            val noteBoxHeight = textLayout.height + 24f
            val noteBoxRect = RectF(PADDING, currentY, PADDING + contentWidth, currentY + noteBoxHeight)
            val noteBgPaint = Paint().apply {
                color = surfaceGray
                style = Paint.Style.FILL
            }
            val noteStrokePaint = Paint().apply {
                color = borderGray
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(noteBoxRect, 6f, 6f, noteBgPaint)
            canvas.drawRoundRect(noteBoxRect, 6f, 6f, noteStrokePaint)

            canvas.save()
            canvas.translate(PADDING + 12f, currentY + 12f)
            textLayout.draw(canvas)
            canvas.restore()

            currentY += noteBoxHeight + 24f

            // 4. EVIDENCIA FOTOGRÁFICA
            val foto = relevamiento.fotoUrl
            if (!foto.isNullOrBlank()) {
                val bitmap = loadBitmap(context, foto)
                if (bitmap != null) {
                    // Check if it fits on page 1, else create new page
                    val maxImgHeight = 220f
                    if (currentY + maxImgHeight + 100f > PAGE_HEIGHT - PADDING) {
                        drawFooter(canvas, 1)
                        pdfDocument.finishPage(currentPage)
                        currentPage = pdfDocument.startPage(pageInfo)
                        canvas = currentPage.canvas
                        currentY = PADDING
                    }

                    canvas.drawText("EVIDENCIA FOTOGRÁFICA ADJUNTA", PADDING, currentY, titlePaint)
                    currentY += 14f

                    val scale = minOf(contentWidth.toFloat() / bitmap.width, maxImgHeight / bitmap.height)
                    val drawW = (bitmap.width * scale).toInt()
                    val drawH = (bitmap.height * scale).toInt()
                    val startX = PADDING + (contentWidth - drawW) / 2f

                    val imgRect = RectF(startX, currentY, startX + drawW, currentY + drawH)
                    val imgBorderPaint = Paint().apply {
                        color = borderGray
                        style = Paint.Style.STROKE
                        strokeWidth = 1f
                    }

                    canvas.drawBitmap(bitmap, null, imgRect, null)
                    canvas.drawRoundRect(imgRect, 4f, 4f, imgBorderPaint)
                    currentY += drawH + 20f
                }
            }

            // 5. CONSTANCIA Y FIRMAS
            if (currentY + 100f > PAGE_HEIGHT - PADDING - 40f) {
                drawFooter(canvas, 1)
                pdfDocument.finishPage(currentPage)
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                currentY = PADDING
            }

            drawSignatures(canvas, currentY, contentWidth)
            drawFooter(canvas, 1)
            pdfDocument.finishPage(currentPage)

            // Guardar en cacheDir y retornar Uri con FileProvider
            val file = File(context.cacheDir, "acta_visita_${obra?.nombre?.replace(" ", "_") ?: relevamiento.id}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))

            val authority = "${context.packageName}.fileprovider"
            return FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument?.close()
        }
    }

    private class PdfPageManager(
        private val pdfDocument: PdfDocument,
        private val obraNombre: String
    ) {
        var pageNumber = 1
            private set
        private var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var currentPage: PdfDocument.Page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = currentPage.canvas
        var currentY: Float = PADDING

        fun ensureSpace(neededHeight: Float, contentWidth: Int) {
            if (currentY + neededHeight > PAGE_HEIGHT - PADDING - 40f) {
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(currentPage)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                currentY = PADDING
                currentY = drawContinuationHeader(canvas, obraNombre, currentY, contentWidth)
            }
        }

        fun finish() {
            drawFooter(canvas, pageNumber)
            pdfDocument.finishPage(currentPage)
        }
    }

    @Suppress("DEPRECATION")
    fun exportInformeObraToPdf(
        context: Context,
        obra: Obra,
        resumenTareas: ResumenTareas?,
        relevamientos: List<Relevamiento>,
        tareas: List<Tarea> = emptyList()
    ): Uri? {
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = PdfDocument()
            val pageManager = PdfPageManager(pdfDocument, obra.nombre)
            val contentWidth = (PAGE_WIDTH - (PADDING * 2)).toInt()

            // 1. HEADER (Página 1)
            pageManager.currentY = drawHeader(
                context = context,
                canvas = pageManager.canvas,
                title = "INFORME EJECUTIVO DE AVANCE DE OBRA",
                subtitle = "Supervisión técnica, control de cronograma y auditoría fotográfica",
                startY = pageManager.currentY,
                contentWidth = contentWidth
            )

            // 2. DATOS GENERALES DE LA OBRA
            pageManager.currentY = drawObraMetaTable(
                canvas = pageManager.canvas,
                obra = obra,
                startY = pageManager.currentY,
                contentWidth = contentWidth
            )

            pageManager.currentY += 20f

            // 3. ESTADO DE CRONOGRAMA Y CONTROL DE TAREAS
            pageManager.ensureSpace(120f, contentWidth)
            val titlePaint = TextPaint().apply {
                color = primaryNavy
                textSize = 13f
                isFakeBoldText = true
            }
            pageManager.canvas.drawText("ESTADO DE CRONOGRAMA Y CONTROL DE TAREAS", PADDING, pageManager.currentY, titlePaint)
            pageManager.currentY += 14f

            val totalTareas = (resumenTareas?.completadas ?: 0) + (resumenTareas?.pendientes ?: 0)
            if (resumenTareas != null && totalTareas > 0) {
                pageManager.currentY = drawCronogramaSummaryCards(
                    canvas = pageManager.canvas,
                    resumen = resumenTareas,
                    startY = pageManager.currentY,
                    contentWidth = contentWidth
                )
            } else {
                val emptyPaint = TextPaint().apply {
                    color = textMuted
                    textSize = 10f
                }
                pageManager.canvas.drawText("No hay tareas registradas en el cronograma.", PADDING, pageManager.currentY, emptyPaint)
                pageManager.currentY += 18f
            }

            pageManager.currentY += 16f

            // 4. DESGLOSE DEL PLAN DE TRABAJO (SI HAY TAREAS)
            if (tareas.isNotEmpty()) {
                pageManager.ensureSpace(80f, contentWidth)
                pageManager.canvas.drawText("DESGLOSE DEL PLAN DE TRABAJO Y TAREAS", PADDING, pageManager.currentY, titlePaint)
                pageManager.currentY += 14f

                pageManager.currentY = drawTareasTable(
                    pageManager = pageManager,
                    tareas = tareas,
                    contentWidth = contentWidth
                )
                pageManager.currentY += 16f
            }

            // 5. HISTORIAL DE RELEVAMIENTOS TÉCNICOS
            pageManager.ensureSpace(80f, contentWidth)
            pageManager.canvas.drawText("HISTORIAL DE VISITAS Y AUDITORÍA DE CAMPO", PADDING, pageManager.currentY, titlePaint)
            pageManager.currentY += 14f

            if (relevamientos.isEmpty()) {
                val emptyPaint = TextPaint().apply {
                    color = textMuted
                    textSize = 10f
                }
                pageManager.canvas.drawText("Esta obra todavía no registra relevamientos de campo.", PADDING, pageManager.currentY, emptyPaint)
                pageManager.currentY += 24f
            } else {
                pageManager.currentY = drawRelevamientosTable(
                    pageManager = pageManager,
                    relevamientos = relevamientos,
                    contentWidth = contentWidth
                )
                pageManager.currentY += 16f
            }

            // 6. ANEXO FOTOGRÁFICO DE EVIDENCIAS TÉCNICAS
            val relevamientosConFoto = relevamientos.filter { !it.fotoUrl.isNullOrBlank() }
            if (relevamientosConFoto.isNotEmpty()) {
                pageManager.ensureSpace(220f, contentWidth)
                val anexoTitlePaint = TextPaint().apply {
                    color = primaryNavy
                    textSize = 14f
                    isFakeBoldText = true
                }
                pageManager.canvas.drawText("ANEXO FOTOGRÁFICO — EVIDENCIAS TÉCNICAS EN TERRENO", PADDING, pageManager.currentY, anexoTitlePaint)
                pageManager.currentY += 14f

                val anexoSubPaint = TextPaint().apply {
                    color = textMuted
                    textSize = 9.5f
                }
                pageManager.canvas.drawText("Registro fotográfico de inspección, georreferenciación y dictámenes técnicos de obra", PADDING, pageManager.currentY, anexoSubPaint)
                pageManager.currentY += 16f

                relevamientosConFoto.forEachIndexed { index, rel ->
                    drawPhotoEvidenceCard(
                        context = context,
                        pageManager = pageManager,
                        relevamiento = rel,
                        index = index,
                        tareas = tareas,
                        contentWidth = contentWidth
                    )
                }
            }

            // 7. FIRMAS Y CONSTANCIA TÉCNICA
            pageManager.ensureSpace(120f, contentWidth)
            val disclaimerPaint = TextPaint().apply {
                color = textMuted
                textSize = 8.5f
                textAlign = Paint.Align.CENTER
            }
            pageManager.canvas.drawText(
                "El presente informe reviste carácter de constancia técnica de supervisión de los trabajos relevados en campo en las fechas indicadas.",
                PAGE_WIDTH / 2f,
                pageManager.currentY + 10f,
                disclaimerPaint
            )
            pageManager.currentY += 20f

            drawSignatures(pageManager.canvas, pageManager.currentY, contentWidth)
            pageManager.finish()

            val file = File(context.cacheDir, "informe_obra_${obra.nombre.replace(" ", "_")}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))

            val authority = "${context.packageName}.fileprovider"
            return FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument?.close()
        }
    }

    // --- MÉTODOS DE DIBUJO MODULARES Y LIMPIOS ---

    private fun drawHeader(
        context: Context,
        canvas: Canvas,
        title: String,
        subtitle: String,
        startY: Float,
        contentWidth: Int
    ): Float {
        var y = startY

        // Logo
        val logoId = context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)
        if (logoId != 0) {
            val bitmap = BitmapFactory.decodeResource(context.resources, logoId)
            if (bitmap != null) {
                val scaled = Bitmap.createScaledBitmap(bitmap, 40, 40, true)
                canvas.drawBitmap(scaled, PADDING, y, null)
            }
        }

        val brandPaint = TextPaint().apply {
            color = accentAmber
            textSize = 18f
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("ObraTrack", PADDING + 48f, y + 26f, brandPaint)

        // Fecha de emisión alineada a la derecha
        val fechaEmision = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val datePaint = TextPaint().apply {
            color = textMuted
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Emitido: $fechaEmision", PADDING + contentWidth, y + 26f, datePaint)

        y += 56f

        // Título del documento
        val docTitlePaint = TextPaint().apply {
            color = primaryNavy
            textSize = 18f
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(title, PADDING, y, docTitlePaint)
        y += 16f

        val docSubTitlePaint = TextPaint().apply {
            color = textMuted
            textSize = 11f
        }
        canvas.drawText(subtitle, PADDING, y, docSubTitlePaint)
        y += 18f

        // Línea divisoria de cabecera
        val linePaint = Paint().apply {
            color = primaryNavy
            strokeWidth = 2f
        }
        canvas.drawLine(PADDING, y, PADDING + contentWidth, y, linePaint)
        y += 18f

        return y
    }

    private fun drawRelevamientoMetaTable(
        canvas: Canvas,
        obra: Obra?,
        relevamiento: Relevamiento,
        tareaTitulo: String?,
        startY: Float,
        contentWidth: Int
    ): Float {
        var y = startY
        val rowHeight = 24f
        val col1Width = contentWidth * 0.35f

        val labelPaint = TextPaint().apply {
            color = primaryNavy
            textSize = 11f
            isFakeBoldText = true
        }
        val valuePaint = TextPaint().apply {
            color = textDark
            textSize = 11f
        }
        val linePaint = Paint().apply {
            color = borderGray
            strokeWidth = 1f
        }

        fun drawRow(label: String, value: String) {
            val bgPaint = Paint().apply {
                color = surfaceGray
                style = Paint.Style.FILL
            }
            canvas.drawRect(PADDING, y, PADDING + contentWidth, y + rowHeight, bgPaint)
            canvas.drawText(label, PADDING + 8f, y + 16f, labelPaint)
            canvas.drawText(value, PADDING + col1Width + 8f, y + 16f, valuePaint)
            canvas.drawLine(PADDING, y + rowHeight, PADDING + contentWidth, y + rowHeight, linePaint)
            y += rowHeight
        }

        drawRow("Obra", obra?.nombre ?: "Sin asignar")
        drawRow("Comitente / Cliente", obra?.cliente ?: "No especificado")
        drawRow("Dirección", obra?.direccion ?: "No especificada")
        drawRow("Fecha de Visita", formatFecha(relevamiento.fecha))

        val gpsText = relevamiento.ubicacion?.let {
            String.format(Locale.US, "%.5f, %.5f (Georreferenciado)", it.latitud, it.longitud)
        } ?: "No registrada en campo"
        drawRow("Ubicación GPS", gpsText)

        if (!tareaTitulo.isNullOrBlank()) {
            drawRow("Tarea Asociada", tareaTitulo)
        }

        relevamiento.proximaVisita?.let {
            drawRow("Próxima Visita Programada", formatFecha(it))
        }

        return y
    }

    private fun drawObraMetaTable(
        canvas: Canvas,
        obra: Obra,
        startY: Float,
        contentWidth: Int
    ): Float {
        var y = startY
        val rowHeight = 24f
        val col1Width = contentWidth * 0.35f

        val labelPaint = TextPaint().apply {
            color = primaryNavy
            textSize = 11f
            isFakeBoldText = true
        }
        val valuePaint = TextPaint().apply {
            color = textDark
            textSize = 11f
        }
        val linePaint = Paint().apply {
            color = borderGray
            strokeWidth = 1f
        }

        fun drawRow(label: String, value: String) {
            val bgPaint = Paint().apply {
                color = surfaceGray
                style = Paint.Style.FILL
            }
            canvas.drawRect(PADDING, y, PADDING + contentWidth, y + rowHeight, bgPaint)
            canvas.drawText(label, PADDING + 8f, y + 16f, labelPaint)
            canvas.drawText(value, PADDING + col1Width + 8f, y + 16f, valuePaint)
            canvas.drawLine(PADDING, y + rowHeight, PADDING + contentWidth, y + rowHeight, linePaint)
            y += rowHeight
        }

        drawRow("Obra", obra.nombre)
        drawRow("Comitente / Cliente", obra.cliente)
        drawRow("Dirección", obra.direccion)
        drawRow("Estado de Obra", obra.estado.name)
        drawRow("Fecha de Inicio", formatFecha(obra.fechaInicio))
        drawRow("Última Inspección", obra.ultimaVisita?.let { formatFecha(it) } ?: "Sin visitas registradas")

        obra.proximaVisita?.let {
            drawRow("Próxima Inspección", formatFecha(it))
        }

        return y
    }

    private fun drawCronogramaSummaryCards(
        canvas: Canvas,
        resumen: ResumenTareas,
        startY: Float,
        contentWidth: Int
    ): Float {
        var y = startY
        val cardWidth = (contentWidth - 24f) / 4f
        val cardHeight = 48f

        val totalTareas = resumen.completadas + resumen.pendientes
        val cards = listOf(
            Triple("TOTAL", "$totalTareas", primaryNavy),
            Triple("COMPLETADAS", "${resumen.completadas}", successGreen),
            Triple("PENDIENTES", "${resumen.pendientes}", primaryNavy),
            Triple("ATRASADAS", "${resumen.atrasadas}", if (resumen.atrasadas > 0) alertRed else textMuted)
        )

        val cardBgPaint = Paint().apply {
            color = surfaceGray
            style = Paint.Style.FILL
        }
        val cardStrokePaint = Paint().apply {
            color = borderGray
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val labelPaint = TextPaint().apply {
            textSize = 9f
            color = textMuted
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        cards.forEachIndexed { index, (label, value, color) ->
            val left = PADDING + (index * (cardWidth + 8f))
            val rect = RectF(left, y, left + cardWidth, y + cardHeight)
            canvas.drawRoundRect(rect, 4f, 4f, cardBgPaint)
            canvas.drawRoundRect(rect, 4f, 4f, cardStrokePaint)

            val valPaint = TextPaint().apply {
                this.color = color
                textSize = 16f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(value, left + cardWidth / 2f, y + 24f, valPaint)
            canvas.drawText(label, left + cardWidth / 2f, y + 38f, labelPaint)
        }

        y += cardHeight + 14f

        // Barra de avance porcentual
        val pct = if (totalTareas > 0) (resumen.completadas.toFloat() / totalTareas) else 0f
        val pctText = "${(pct * 100).toInt()}% de avance físico"

        val pctPaint = TextPaint().apply {
            color = textDark
            textSize = 10f
            isFakeBoldText = true
        }
        canvas.drawText(pctText, PADDING, y, pctPaint)
        y += 6f

        val barHeight = 8f
        val barBgPaint = Paint().apply {
            color = borderGray
            style = Paint.Style.FILL
        }
        val barFillPaint = Paint().apply {
            color = successGreen
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(PADDING, y, PADDING + contentWidth, y + barHeight), 4f, 4f, barBgPaint)
        if (pct > 0f) {
            canvas.drawRoundRect(RectF(PADDING, y, PADDING + (contentWidth * pct), y + barHeight), 4f, 4f, barFillPaint)
        }
        y += barHeight + 12f

        return y
    }

    private fun drawContinuationHeader(
        canvas: Canvas,
        obraNombre: String,
        startY: Float,
        contentWidth: Int
    ): Float {
        var y = startY
        val brandPaint = TextPaint().apply {
            color = accentAmber
            textSize = 12f
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("ObraTrack", PADDING, y + 12f, brandPaint)

        val docTitlePaint = TextPaint().apply {
            color = primaryNavy
            textSize = 10f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Informe Técnico: $obraNombre (Continuación)", PADDING + contentWidth, y + 12f, docTitlePaint)
        y += 18f

        val linePaint = Paint().apply {
            color = borderGray
            strokeWidth = 1f
        }
        canvas.drawLine(PADDING, y, PADDING + contentWidth, y, linePaint)
        y += 16f
        return y
    }

    private fun drawTareasTable(
        pageManager: PdfPageManager,
        tareas: List<Tarea>,
        contentWidth: Int
    ): Float {
        val colTitulo = contentWidth * 0.40f
        val colCat = contentWidth * 0.22f
        val colFechas = contentWidth * 0.20f
        val colEstado = contentWidth * 0.18f
        val rowHeight = 22f

        val thPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 9.5f
            isFakeBoldText = true
        }
        val thBg = Paint().apply {
            color = primaryNavy
            style = Paint.Style.FILL
        }

        pageManager.canvas.drawRect(PADDING, pageManager.currentY, PADDING + contentWidth, pageManager.currentY + 18f, thBg)
        pageManager.canvas.drawText("Tarea", PADDING + 6f, pageManager.currentY + 13f, thPaint)
        pageManager.canvas.drawText("Categoría", PADDING + colTitulo + 6f, pageManager.currentY + 13f, thPaint)
        pageManager.canvas.drawText("Plazo", PADDING + colTitulo + colCat + 6f, pageManager.currentY + 13f, thPaint)
        pageManager.canvas.drawText("Estado", PADDING + colTitulo + colCat + colFechas + 6f, pageManager.currentY + 13f, thPaint)
        pageManager.currentY += 18f

        val tdPaint = TextPaint().apply {
            color = textDark
            textSize = 9f
        }
        val linePaint = Paint().apply {
            color = borderGray
            strokeWidth = 0.8f
        }

        tareas.forEach { tarea ->
            pageManager.ensureSpace(rowHeight, contentWidth)
            val y = pageManager.currentY
            val titStr = if (tarea.titulo.length > 34) tarea.titulo.take(32) + "..." else tarea.titulo
            val catStr = tarea.categoria.name.lowercase().replaceFirstChar { it.uppercase() }
            val inicio = tarea.fechaInicio
            val fin = tarea.fechaFin
            val plazoStr = when {
                inicio != null && fin != null -> "${formatFecha(inicio)} - ${formatFecha(fin)}"
                fin != null -> "Fin: ${formatFecha(fin)}"
                inicio != null -> "Ini: ${formatFecha(inicio)}"
                else -> "-"
            }

            pageManager.canvas.drawText(titStr, PADDING + 6f, y + 14f, tdPaint)
            pageManager.canvas.drawText(catStr, PADDING + colTitulo + 6f, y + 14f, tdPaint)
            pageManager.canvas.drawText(plazoStr, PADDING + colTitulo + colCat + 6f, y + 14f, tdPaint)

            val (badgeBg, badgeText, badgeLabel) = when (tarea.estado) {
                EstadoTarea.COMPLETADA -> Triple(chipBgCompletada, chipTextCompletada, "Completada")
                EstadoTarea.EN_CURSO -> Triple(chipBgEnCurso, chipTextEnCurso, "En curso")
                EstadoTarea.PENDIENTE -> Triple(chipBgPending, chipTextPending, "Pendiente")
            }
            val badgeX = PADDING + colTitulo + colCat + colFechas + 4f
            val badgeRect = RectF(badgeX, y + 3f, badgeX + colEstado - 8f, y + 17f)
            val badgePaint = Paint().apply {
                color = badgeBg
                style = Paint.Style.FILL
            }
            val badgeTextPaint = TextPaint().apply {
                color = badgeText
                textSize = 8f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            pageManager.canvas.drawRoundRect(badgeRect, 3f, 3f, badgePaint)
            pageManager.canvas.drawText(badgeLabel, badgeRect.centerX(), y + 13f, badgeTextPaint)

            pageManager.canvas.drawLine(PADDING, y + rowHeight, PADDING + contentWidth, y + rowHeight, linePaint)
            pageManager.currentY += rowHeight
        }

        return pageManager.currentY
    }

    private fun drawRelevamientosTable(
        pageManager: PdfPageManager,
        relevamientos: List<Relevamiento>,
        contentWidth: Int
    ): Float {
        val colFecha = contentWidth * 0.18f
        val colObs = contentWidth * 0.52f
        val colFoto = contentWidth * 0.15f
        val colProx = contentWidth * 0.15f
        val rowHeight = 24f

        val thPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 9.5f
            isFakeBoldText = true
        }
        val thBg = Paint().apply {
            color = primaryNavy
            style = Paint.Style.FILL
        }

        pageManager.canvas.drawRect(PADDING, pageManager.currentY, PADDING + contentWidth, pageManager.currentY + 18f, thBg)
        pageManager.canvas.drawText("Fecha", PADDING + 6f, pageManager.currentY + 13f, thPaint)
        pageManager.canvas.drawText("Observaciones y Hallazgos", PADDING + colFecha + 6f, pageManager.currentY + 13f, thPaint)
        pageManager.canvas.drawText("Evidencia", PADDING + colFecha + colObs + 6f, pageManager.currentY + 13f, thPaint)
        pageManager.canvas.drawText("Próx. Visita", PADDING + colFecha + colObs + colFoto + 6f, pageManager.currentY + 13f, thPaint)
        pageManager.currentY += 18f

        val tdPaint = TextPaint().apply {
            color = textDark
            textSize = 9f
        }
        val linePaint = Paint().apply {
            color = borderGray
            strokeWidth = 0.8f
        }

        relevamientos.forEach { rel ->
            pageManager.ensureSpace(rowHeight, contentWidth)
            val y = pageManager.currentY
            val fechaStr = formatFecha(rel.fecha)
            val obsStr = if (rel.nota.length > 50) rel.nota.take(47) + "..." else rel.nota.ifBlank { "Sin notas" }
            val fotoStr = if (!rel.fotoUrl.isNullOrBlank()) "📷 Adjunta" else "-"
            val proxStr = rel.proximaVisita?.let { formatFecha(it) } ?: "-"

            pageManager.canvas.drawText(fechaStr, PADDING + 6f, y + 15f, tdPaint)
            pageManager.canvas.drawText(obsStr, PADDING + colFecha + 6f, y + 15f, tdPaint)
            pageManager.canvas.drawText(fotoStr, PADDING + colFecha + colObs + 6f, y + 15f, tdPaint)
            pageManager.canvas.drawText(proxStr, PADDING + colFecha + colObs + colFoto + 6f, y + 15f, tdPaint)

            pageManager.canvas.drawLine(PADDING, y + rowHeight, PADDING + contentWidth, y + rowHeight, linePaint)
            pageManager.currentY += rowHeight
        }

        return pageManager.currentY
    }

    private fun drawPhotoEvidenceCard(
        context: Context,
        pageManager: PdfPageManager,
        relevamiento: Relevamiento,
        index: Int,
        tareas: List<Tarea>,
        contentWidth: Int
    ) {
        val photoBoxWidth = 200f
        val photoBoxHeight = 150f
        val rightColWidth = contentWidth - photoBoxWidth - 24f
        val rightColLeft = PADDING + photoBoxWidth + 16f

        val titleText = "REGISTRO FOTOGRÁFICO DE CAMPO #${index + 1}"
        val fechaText = "Fecha de Captura: ${formatFecha(relevamiento.fecha)}"
        val tareaText = if (!relevamiento.tareaId.isNullOrBlank()) {
            val matched = tareas.find { it.id == relevamiento.tareaId }
            "Tarea Vinculada: ${matched?.titulo ?: "Ref #${relevamiento.tareaId}"}"
        } else {
            "Tarea Vinculada: Supervisión General de Obra"
        }
        val gpsText = relevamiento.ubicacion?.let {
            String.format(Locale.US, "GPS: %.5f, %.5f", it.latitud, it.longitud)
        } ?: "GPS: No georreferenciado en campo"

        val noteText = relevamiento.nota.ifBlank { "Sin observaciones adicionales registradas durante la captura de esta evidencia." }

        val notePaint = TextPaint().apply {
            color = textDark
            textSize = 9.5f
        }
        val textLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(noteText, 0, noteText.length, notePaint, rightColWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.15f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(noteText, notePaint, rightColWidth.toInt(), Layout.Alignment.ALIGN_NORMAL, 1.15f, 0f, false)
        }

        val rightContentHeight = 65f + textLayout.height
        val cardHeight = maxOf(photoBoxHeight + 20f, rightContentHeight + 16f)

        pageManager.ensureSpace(cardHeight + 14f, contentWidth)
        val y = pageManager.currentY

        val cardRect = RectF(PADDING, y, PADDING + contentWidth, y + cardHeight)
        val cardBgPaint = Paint().apply {
            color = surfaceGray
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint().apply {
            color = borderGray
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        pageManager.canvas.drawRoundRect(cardRect, 6f, 6f, cardBgPaint)
        pageManager.canvas.drawRoundRect(cardRect, 6f, 6f, cardBorderPaint)

        // Marco contenedor de la fotografía
        val photoFrameRect = RectF(PADDING + 10f, y + 10f, PADDING + 10f + photoBoxWidth, y + 10f + photoBoxHeight)
        val photoFrameBg = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            style = Paint.Style.FILL
        }
        pageManager.canvas.drawRoundRect(photoFrameRect, 4f, 4f, photoFrameBg)

        val photoUrl = relevamiento.fotoUrl
        val bitmap = if (!photoUrl.isNullOrBlank()) loadBitmap(context, photoUrl) else null

        if (bitmap != null) {
            val scale = minOf(photoBoxWidth / bitmap.width, photoBoxHeight / bitmap.height)
            val drawW = bitmap.width * scale
            val drawH = bitmap.height * scale
            val startX = photoFrameRect.left + (photoBoxWidth - drawW) / 2f
            val startY = photoFrameRect.top + (photoBoxHeight - drawH) / 2f
            val imgRect = RectF(startX, startY, startX + drawW, startY + drawH)

            pageManager.canvas.drawBitmap(bitmap, null, imgRect, null)
            val imgBorder = Paint().apply {
                color = borderGray
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            pageManager.canvas.drawRoundRect(imgRect, 3f, 3f, imgBorder)
        } else {
            val emptyPhotoPaint = TextPaint().apply {
                color = textMuted
                textSize = 9.5f
                textAlign = Paint.Align.CENTER
            }
            pageManager.canvas.drawText("Fotografía no disponible", photoFrameRect.centerX(), photoFrameRect.centerY() + 3f, emptyPhotoPaint)
        }

        // Metadatos y notas técnicas en la columna derecha
        var curTextY = y + 22f

        val cardTitlePaint = TextPaint().apply {
            color = primaryNavy
            textSize = 10.5f
            isFakeBoldText = true
        }
        pageManager.canvas.drawText(titleText, rightColLeft, curTextY, cardTitlePaint)
        curTextY += 14f

        val metaPaint = TextPaint().apply {
            color = textMuted
            textSize = 8.5f
        }
        pageManager.canvas.drawText(fechaText, rightColLeft, curTextY, metaPaint)
        curTextY += 12f

        pageManager.canvas.drawText(tareaText, rightColLeft, curTextY, metaPaint)
        curTextY += 12f

        pageManager.canvas.drawText(gpsText, rightColLeft, curTextY, metaPaint)
        curTextY += 15f

        val dictamenLabelPaint = TextPaint().apply {
            color = primaryNavy
            textSize = 9f
            isFakeBoldText = true
        }
        pageManager.canvas.drawText("Dictamen y Observación Técnica:", rightColLeft, curTextY, dictamenLabelPaint)
        curTextY += 8f

        pageManager.canvas.save()
        pageManager.canvas.translate(rightColLeft, curTextY)
        textLayout.draw(pageManager.canvas)
        pageManager.canvas.restore()

        pageManager.currentY += cardHeight + 14f
    }

    private fun drawSignatures(canvas: Canvas, y: Float, contentWidth: Int) {
        val signWidth = 180f
        val linePaint = Paint().apply {
            color = textDark
            strokeWidth = 1f
        }
        val signTextPaint = TextPaint().apply {
            color = textDark
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }

        // Firma Profesional
        val left1 = PADDING + 20f
        canvas.drawLine(left1, y + 40f, left1 + signWidth, y + 40f, linePaint)
        canvas.drawText("Firma del Profesional Responsable", left1 + (signWidth / 2f), y + 54f, signTextPaint)

        // Firma Comitente
        val left2 = PADDING + contentWidth - signWidth - 20f
        canvas.drawLine(left2, y + 40f, left2 + signWidth, y + 40f, linePaint)
        canvas.drawText("Conformidad Comitente / Capataz", left2 + (signWidth / 2f), y + 54f, signTextPaint)
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int) {
        val footerPaint = TextPaint().apply {
            color = textMuted
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "ObraTrack — Documento de supervisión y auditoría técnica de obra. Pág. $pageNumber",
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 20f,
            footerPaint
        )
    }

    private fun loadBitmap(context: Context, urlOrPath: String): Bitmap? {
        if (urlOrPath.isBlank()) return null
        // 1. Verificación directa de archivo local o URI file://
        try {
            val localPath = when {
                urlOrPath.startsWith("file://") -> Uri.parse(urlOrPath).path
                urlOrPath.startsWith("/") -> urlOrPath
                else -> null
            }
            if (localPath != null) {
                val file = File(localPath)
                if (file.exists() && file.isFile && file.length() > 0) {
                    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
                    var sampleSize = 1
                    while ((boundsOpts.outWidth / sampleSize) > 1200 || (boundsOpts.outHeight / sampleSize) > 1200) {
                        sampleSize *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val bmp = BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                    if (bmp != null) return bmp
                }
            }
        } catch (_: Exception) { }

        // 2. Ejecución con Coil (URLs remotas, Cloudinary, Firebase, content:// URIs)
        return try {
            val imageLoader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(urlOrPath)
                .size(1024, 1024)
                .allowHardware(false)
                .build()
            val result = runBlocking(Dispatchers.IO) {
                imageLoader.execute(request)
            }
            if (result is SuccessResult) {
                result.drawable.toBitmap()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}