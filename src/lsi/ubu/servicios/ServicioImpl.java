package lsi.ubu.servicios;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lsi.ubu.excepciones.CompraBilleteTrenException;
import lsi.ubu.util.PoolDeConexiones;

public class ServicioImpl implements Servicio {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServicioImpl.class);

	@Override
	public void anularBillete(Time hora, java.util.Date fecha, String origen, String destino, int nroPlazas, int ticket)
			throws SQLException {
		PoolDeConexiones pool = PoolDeConexiones.getInstance();

		/* Conversiones de fechas y horas */
		java.sql.Date fechaSqlDate = new java.sql.Date(fecha.getTime());
		java.sql.Timestamp horaTimestamp = new java.sql.Timestamp(hora.getTime());

		Connection con = null;
		PreparedStatement SEL_viajes = null;
		PreparedStatement SEL_ticket = null;
		PreparedStatement UPD_viajes = null;
		PreparedStatement DEL_ticket = null;

		ResultSet rs = null;
		ResultSet rs2 = null;

		int rowCount = -1;

		// A completar por el alumno

		try {
			con = pool.getConnection();

			// recogida del idViaje y precio con una consulta.
			SEL_viajes = con.prepareStatement("SELECT idViaje, precio FROM recorridos natural join viajes "
					+ "WHERE horaSalida-trunc(horaSalida) = ?-trunc(?) AND trunc(fecha) = trunc(?) "
					+ "AND estacionOrigen = ? AND estacionDestino = ? ");

			SEL_viajes.setTimestamp(1, horaTimestamp);
			SEL_viajes.setTimestamp(2, horaTimestamp);
			SEL_viajes.setDate(3, fechaSqlDate);
			SEL_viajes.setString(4, origen);
			SEL_viajes.setString(5, destino);

			rs = SEL_viajes.executeQuery();

			if (!rs.next())
				throw new CompraBilleteTrenException(CompraBilleteTrenException.NO_EXISTE_VIAJE);

			// comprobar si billete existe
			SEL_ticket = con.prepareStatement("SELECT COUNT(*) FROM tickets WHERE idTicket = ?");
			SEL_ticket.setInt(1, ticket);
			rs2 = SEL_ticket.executeQuery();

			if (rs2.next() && rs2.getInt(1) <= 0) {
				throw new AnularBilleteTrenException(AnularBilleteTrenException.NO_EXISTE);
			}

			// Insert del ticket con el nuevo viaje
			DEL_ticket = con.prepareStatement("DELETE FROM tickets WHERE idTicket = ?");
			DEL_ticket.setInt(1, ticket);

			DEL_ticket.executeUpdate();

			// Update de la tabla viajes
			UPD_viajes = con.prepareStatement("UPDATE viajes SET nPlazaslibres = nPlazaslibres + ? WHERE idViaje = ?");
			UPD_viajes.setInt(1, nroPlazas);
			UPD_viajes.setInt(2, rs.getInt(1));
			rowCount = UPD_viajes.executeUpdate();

			if (rowCount == 0)
				throw new SQLException();

			con.commit();

		} catch (SQLException e) {
			con.rollback();
			throw e;

		} finally {
			if (rs != null)
				rs.close();

			if (DEL_ticket != null)
				DEL_ticket.close();
			if (UPD_viajes != null)
				UPD_viajes.close();
			if (SEL_viajes != null)
				SEL_viajes.close();
			if (SEL_ticket != null)
				SEL_ticket.close();

			if (con != null)
				con.close();
		}
	}

	@Override
	public void comprarBillete(Time hora, Date fecha, String origen, String destino, int nroPlazas)
			throws SQLException {
		PoolDeConexiones pool = PoolDeConexiones.getInstance();

		/* Conversiones de fechas y horas */
		java.sql.Date fechaSqlDate = new java.sql.Date(fecha.getTime());
		java.sql.Timestamp horaTimestamp = new java.sql.Timestamp(hora.getTime());

		Connection con = null;
		PreparedStatement SEL_viajes = null;
		PreparedStatement UPD_viajes = null;
		PreparedStatement INS_ticket = null;

		ResultSet rs = null;

		int rowCount = -1;
		int rowCount2 = -1;

		// A completar por el alumno
		try {
			con = pool.getConnection();

			// recogida del idViaje y precio con una consulta.
			SEL_viajes = con.prepareStatement("SELECT idViaje, precio FROM recorridos natural join viajes "
					+ "WHERE horaSalida-trunc(horaSalida) = ?-trunc(?) AND trunc(fecha) = trunc(?) "
					+ "AND estacionOrigen = ? AND estacionDestino = ? ");

			SEL_viajes.setTimestamp(1, horaTimestamp);
			SEL_viajes.setTimestamp(2, horaTimestamp);
			SEL_viajes.setDate(3, fechaSqlDate);
			SEL_viajes.setString(4, origen);
			SEL_viajes.setString(5, destino);

			rs = SEL_viajes.executeQuery();

			if (!rs.next())
				throw new CompraBilleteTrenException(CompraBilleteTrenException.NO_EXISTE_VIAJE);

			// Update de la tabla viajes
			UPD_viajes = con.prepareStatement("UPDATE viajes SET nPlazasLibres = nPlazasLibres - ? "
					+ "WHERE idViaje = ? AND ? <= nPlazasLibres");
			UPD_viajes.setInt(1, nroPlazas);
			UPD_viajes.setInt(2, rs.getInt("idViaje"));
			UPD_viajes.setInt(3, nroPlazas);
			rowCount = UPD_viajes.executeUpdate();

			if (rowCount == 0)
				throw new CompraBilleteTrenException(CompraBilleteTrenException.NO_PLAZAS);

			// Insert del ticket con el nuevo viaje
			INS_ticket = con
					.prepareStatement("INSERT INTO tickets VALUES " + "(SEQ_TICKETS.nextval, ?, current_date, ?, ?)");
			INS_ticket.setInt(1, rs.getInt("idViaje"));
			INS_ticket.setInt(2, nroPlazas);
			INS_ticket.setBigDecimal(3, (rs.getBigDecimal("precio")).multiply(new BigDecimal(nroPlazas)));
			rowCount2 = INS_ticket.executeUpdate();
			if (rowCount2 == 0) {
				throw new SQLException();
			}

		} catch (SQLException e) {
			con.rollback();
			throw e;
		} finally {
			if (rs != null)
				rs.close();

			if (INS_ticket != null)
				INS_ticket.close();
			if (UPD_viajes != null)
				UPD_viajes.close();
			if (SEL_viajes != null)
				SEL_viajes.close();

			if (con != null)
				con.close();

		}
	}

}
