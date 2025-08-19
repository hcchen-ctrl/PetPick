package tw.petpick.petpick.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.petpick.petpick.model.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeNumberAndPassword(String employeeNumber, String password);
}
