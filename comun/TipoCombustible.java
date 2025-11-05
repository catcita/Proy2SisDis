package comun;

/**
 * representa los tipos de combustible disponibles
 */
public enum TipoCombustible {
    GASOLINA_93("93"),
    GASOLINA_95("95"),
    GASOLINA_97("97"),
    DIESEL("Diesel"),
    KEROSENE("Kerosene");

    private final String nombre;

    TipoCombustible(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }

    /**
     * Obtiene el tipo de combustible a partir de su nombre
     */
    public static TipoCombustible fromString(String nombre) {
        for (TipoCombustible tipo : TipoCombustible.values()) {
            if (tipo.nombre.equalsIgnoreCase(nombre)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de combustible no válido: " + nombre);
    }
}