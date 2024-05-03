package lsi.ubu.excepciones;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnularBilleteTrenException extends SQLException {
	
	private static final long serialVersionUID = 2L;
	
	public static final int NO_EXISTE = 1;

	private String mensaje;
	private int codigo;
	
	private static Logger log = LoggerFactory.getLogger(AnularBilleteTrenException.class);	
	
	public AnularBilleteTrenException(int codigo) {
		this.codigo = codigo;
		
		switch (codigo) {
			case NO_EXISTE:
				mensaje = "No existe el billete.";
				break;
		}

		// ----------------------------------------------------//
		log.error(mensaje);

		// Traza_de_pila
		for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			log.info(ste.toString());
		}
	}
	
	
	
	
	@Override
	public String getMessage() { // Redefinicion del metodo de la clase
									// Exception
		return mensaje;
	}

	@Override
	public int getErrorCode() { // Redefinicion del metodo de la clase
								// SQLException
		return codigo;
	}

}

