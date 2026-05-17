package com.nelumbo.reservas.exception;

public class SucursalConSalonesException extends RuntimeException {
    public SucursalConSalonesException(Long id) {
        super("No se puede eliminar la sucursal " + id + " porque tiene salones asociados");
    }
}