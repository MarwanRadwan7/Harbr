package com.harbr.booking.infrastructure;

import com.harbr.booking.domain.Booking;
import com.harbr.booking.domain.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.property.id = :propertyId "
            + "AND b.status NOT IN :excludedStatuses "
            + "AND b.checkIn < :checkOut AND b.checkOut > :checkIn")
    List<Booking> findOverlappingBookingsForUpdate(
            @Param("propertyId") UUID propertyId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("excludedStatuses") List<BookingStatus> excludedStatuses);

    Page<Booking> findByGuestIdAndStatusIn(UUID guestId, List<BookingStatus> statuses, Pageable pageable);

    Page<Booking> findByPropertyHostIdAndStatusIn(UUID hostId, List<BookingStatus> statuses, Pageable pageable);

    Page<Booking> findByGuestId(UUID guestId, Pageable pageable);

    Page<Booking> findByPropertyHostId(UUID hostId, Pageable pageable);
}