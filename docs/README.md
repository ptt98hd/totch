# Tài liệu dự án Totch

| # | Tài liệu | Nội dung |
|---|---|---|
| 01 | [Yêu cầu nghiệp vụ](01-requirements.md) | Tầm nhìn, persona, phạm vi, FR/NFR, rủi ro |
| 02 | [Thiết kế hệ thống](02-architecture.md) | Module, API lõi, luồng, quyền, ADR |
| 03 | [Kiểm thử](03-testing.md) | Quy trình TDD, ma trận truy vết, checklist thủ công |
| 04 | [Phát hành F-Droid](04-release-fdroid.md) | Versioning, build, metadata, quy trình submit |
| 05 | [Checklist test thủ công](05-manual-test-checklist.md) | Kịch bản test trên máy thật, lịch sử kết quả |
| — | [specs/](specs/) | Spec chi tiết theo tính năng, có ID tiêu chí chấp nhận |

## Quy trình làm việc (spec-driven + TDD)
```
yêu cầu (01) → thiết kế (02) → spec + ID (specs/) → test đỏ → code xanh → refactor → cập nhật docs
```
Mọi thay đổi hành vi phải đi qua spec trước. Commit message nên nhắc ID, ví dụ `feat(core): GR-14 second long press`.
