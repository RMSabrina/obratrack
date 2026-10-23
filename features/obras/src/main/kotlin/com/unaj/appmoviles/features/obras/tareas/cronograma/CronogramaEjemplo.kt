package com.unaj.appmoviles.features.obras.tareas.cronograma

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaTarea
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import java.time.LocalDate

// Datos de ejemplo solo para el @Preview (Etapa 3.1). En la 3.2 se reemplazan por las
// tareas reales de la obra.

private val hoyEjemplo: LocalDate = LocalDate.of(2026, 10, 23)

private fun tareaEjemplo(
    id: String,
    titulo: String,
    categoria: CategoriaTarea,
    inicio: LocalDate?,
    fin: LocalDate?,
    estado: EstadoTarea
) = Tarea(
    id = id,
    obraId = "obra-ejemplo",
    userId = "usuario-ejemplo",
    titulo = titulo,
    categoria = categoria,
    fechaInicio = inicio,
    fechaFin = fin,
    estado = estado
)

private val tareasEjemplo = listOf(
    tareaEjemplo("1", "Excavación", CategoriaTarea.CIMIENTOS_Y_EXCAVACION,
        hoyEjemplo.minusDays(20), hoyEjemplo.minusDays(12), EstadoTarea.COMPLETADA),
    tareaEjemplo("2", "Platea", CategoriaTarea.ESTRUCTURA,
        hoyEjemplo.minusDays(11), hoyEjemplo.minusDays(2), EstadoTarea.EN_CURSO), // atrasada
    tareaEjemplo("3", "Mampostería", CategoriaTarea.ALBANILERIA,
        hoyEjemplo.minusDays(3), hoyEjemplo.plusDays(15), EstadoTarea.EN_CURSO),
    tareaEjemplo("4", "Inspección eléctrica", CategoriaTarea.ELECTRICIDAD,
        hoyEjemplo.plusDays(5), hoyEjemplo.plusDays(5), EstadoTarea.PENDIENTE), // un solo día
    tareaEjemplo("5", "Techo", CategoriaTarea.TECHOS,
        hoyEjemplo.plusDays(16), hoyEjemplo.plusDays(30), EstadoTarea.PENDIENTE),
    tareaEjemplo("6", "Limpieza final", CategoriaTarea.LIMPIEZA,
        null, null, EstadoTarea.PENDIENTE) // sin fechas: no se dibuja
)

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun GanttChartPreview() {
    GanttChartEjemplo()
}

// El período de ejemplo tiene 57 días (57 × 24 dp + 128 dp de títulos ≈ 1500 dp):
// con este ancho se ven todas las barras sin scroll.
@Preview(showBackground = true, widthDp = 1500)
@Composable
private fun GanttChartAnchoPreview() {
    GanttChartEjemplo()
}

@Composable
private fun GanttChartEjemplo() {
    MaterialTheme {
        Surface {
            GanttChart(
                cronograma = calcularCronograma(tareasEjemplo, hoyEjemplo),
                onTareaClick = {}
            )
        }
    }
}
