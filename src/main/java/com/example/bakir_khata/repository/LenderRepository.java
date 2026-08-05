package com.example.bakir_khata.repository;


import com.example.bakir_khata.model.Lender;
import com.example.bakir_khata.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LenderRepository extends JpaRepository<Lender, Long> {

    List<Lender> findByUserOrderByNameAsc(User user);

    @Query("""
           select l from Lender l
           where l.user = :user
             and (lower(l.name) like lower(concat('%', :term, '%'))
                  or l.phone like concat('%', :term, '%')
                  or lower(coalesce(l.email, '')) like lower(concat('%', :term, '%')))
           order by l.name asc
           """)
    List<Lender> search(@Param("user") User user, @Param("term") String term);
}
