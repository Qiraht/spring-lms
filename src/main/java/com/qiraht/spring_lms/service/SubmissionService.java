package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.dto.request.GradeRequestDTO;
import com.qiraht.spring_lms.dto.request.SubmissionRequestDTO;
import com.qiraht.spring_lms.dto.response.AuthorDTO;
import com.qiraht.spring_lms.dto.response.SubmissionResponseDTO;
import com.qiraht.spring_lms.entity.Assignment;
import com.qiraht.spring_lms.entity.AssignmentSubmission;
import com.qiraht.spring_lms.entity.User;
import com.qiraht.spring_lms.exception.NotFoundException;
import com.qiraht.spring_lms.repository.AssignmentRepository;
import com.qiraht.spring_lms.repository.AssignmentSubmissionRepository;
import com.qiraht.spring_lms.repository.UserRepository;
import com.qiraht.spring_lms.security.CustomUsersDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    public SubmissionResponseDTO submitAssignment(String assignmentId, SubmissionRequestDTO request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));

        CustomUsersDetails userDetails = (CustomUsersDetails) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Check if previously submitted to update or create new
        Optional<AssignmentSubmission> existingSubmission = submissionRepository
                .findByAssignmentIdAndUserId(assignmentId, user.getId());

        AssignmentSubmission submission;
        if (existingSubmission.isPresent()) {
            submission = existingSubmission.get();
            submission.setAttachment(request.getAttachment());
        } else {
            submission = AssignmentSubmission.builder()
                    .assignment(assignment)
                    .user(user)
                    .attachment(request.getAttachment())
                    .score(BigDecimal.ZERO)
                    .build();
        }

        submissionRepository.save(submission);
        return mapToDTO(submission);
    }

    public List<SubmissionResponseDTO> getAllSubmissions(String assignmentId) {
        assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));

        List<AssignmentSubmission> submissions = submissionRepository.findByAssignmentId(assignmentId);
        return submissions.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public SubmissionResponseDTO getSubmissionById(String submissionId) {
        AssignmentSubmission submission = submissionRepository.findById(UUID.fromString(submissionId))
                .orElseThrow(() -> new NotFoundException("Submission not found"));
        return mapToDTO(submission);
    }

    public SubmissionResponseDTO gradeSubmission(String submissionId, GradeRequestDTO request) {
        AssignmentSubmission submission = submissionRepository.findById(UUID.fromString(submissionId))
                .orElseThrow(() -> new NotFoundException("Submission not found"));

        submission.setScore(request.getScore());
        submissionRepository.save(submission);

        return mapToDTO(submission);
    }

    private SubmissionResponseDTO mapToDTO(AssignmentSubmission submission) {
        SubmissionResponseDTO dto = new SubmissionResponseDTO();
        BeanUtils.copyProperties(submission, dto);
        dto.setId(submission.getId().toString());
        dto.setAssignmentId(submission.getAssignment().getId());
        dto.setSubmittedAt(submission.getCreatedAt());

        if (submission.getUser() != null) {
            dto.setStudent(AuthorDTO.builder()
                    .id(submission.getUser().getId())
                    .firstName(submission.getUser().getFirstName())
                    .lastName(submission.getUser().getLastName())
                    .build());
        }
        return dto;
    }
}
