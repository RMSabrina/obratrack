package com.unaj.appmoviles.obratrack.core.domain.error

/** Por qué falló una operación, sin importar de qué dominio vino el error. La UI elige el mensaje con esto. */
enum class CausaError { SIN_CONEXION, SIN_PERMISO, NO_ENCONTRADO, TARDO_DEMASIADO, OTRA }

fun DomainError.causa(): CausaError = when (this) {
    AuthError.NetworkError, ObraError.Red, RelevamientoError.Red, TareaError.Red -> CausaError.SIN_CONEXION
    ObraError.NoAutorizada, RelevamientoError.NoAutorizado, TareaError.NoAutorizada -> CausaError.SIN_PERMISO
    ObraError.NoEncontrada, RelevamientoError.NoEncontrado, TareaError.NoEncontrada -> CausaError.NO_ENCONTRADO
    ObraError.Timeout, RelevamientoError.Timeout, TareaError.Timeout -> CausaError.TARDO_DEMASIADO
    else -> CausaError.OTRA
}

/**
 * Falla de un Flow de lectura, ya clasificada por la capa de datos. Un Flow solo puede
 * informar un error lanzándolo; el ViewModel lo atrapa con `catch` y lo pasa al estado.
 */
class LecturaFallidaException(val error: DomainError, causa: Throwable) : RuntimeException(causa)

/** Error de dominio de una falla de lectura; si no vino clasificada, [sinClasificar]. */
fun Throwable.aErrorDeLectura(sinClasificar: DomainError = ObraError.Desconocido): DomainError =
    (this as? LecturaFallidaException)?.error ?: sinClasificar
