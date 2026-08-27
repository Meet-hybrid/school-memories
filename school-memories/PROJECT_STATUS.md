# Character Training Secondary School — Project Status

Updated: 27 August 2026

## Purpose

This project is now being prepared as a private 30-day memory challenge and digital archive for Character Training Secondary School. Classmates answer one question each day, share memories, and interact with one another in a school-only archive.

## Achieved

### Product

- Landing page and school-focused branding
- Registration, login, logout, email verification, and password reset flows
- Private school invite-code registration
- User profiles with nickname, set, graduation year, bio, avatar, and statistics
- 30-day challenge with flexible day completion
- Memory posts with text, mood, photos, and videos
- Feed with pagination and school-aware content
- Likes, comments, follows, notifications, and suggested classmates
- Achievements and leaderboards
- Guess Who, School Trivia, and Classmate Bingo games
- Admin dashboard for users, schools, sets, questions, trivia, moderation, and announcements
- Existing legacy school wording migration from Greenfield to Character Training Secondary School

### Technical

- Spring Boot 3.3 backend with Java 21
- PostgreSQL database
- Next.js 14 frontend with TypeScript and Tailwind CSS
- JWT authentication with BCrypt password hashing
- Docker deployment configuration
- Render Blueprint configuration for the backend
- Vercel-ready frontend configuration
- Local or S3-compatible media storage support
- SMTP email support with development logging fallback
- Integration test coverage for the main flows

### Deployment

- Code repository: https://github.com/Meet-hybrid/school-memories
- Backend target: Render Web Service
- Database: Render Free PostgreSQL for the 30-day challenge
- Frontend target: Vercel
- School name: Character Training Secondary School
- Invite code: 2014
- Admin email: admin@ctss

## Remaining before inviting classmates

- Confirm Render backend starts successfully with the correct database URL
- Confirm `SCHOOL_NAME`, `SCHOOL_INVITE_CODE`, and `ADMIN_EMAIL` are set on the Render web service
- Confirm Vercel has `NEXT_PUBLIC_API_URL` pointing to the Render backend
- Redeploy Vercel after the latest frontend commit
- Log in as admin and remove or hide all seeded demo classmates, memories, comments, reactions, follows, and announcements
- Keep real classmates, including George Metherson, active
- Replace local upload storage with Cloudinary, Cloudflare R2, or S3
- Configure production SMTP if classmates need verification and reset emails
- Replace the temporary admin password
- Test registration with invite code `2014`, login, challenge submission, comments, logout, and mobile layout

## Planned after launch

- Polls and throwback photo contests
- Memory of the Week
- Digital yearbook generation
- Time capsules
- Reunion planning and RSVP
- Interactive school map
- Multi-school theming if the project expands
- Better moderation reports and abuse handling
- Database migrations with Flyway or Liquibase
- Automated database backups
- HttpOnly cookie authentication instead of localStorage JWTs
- Video transcoding and streaming optimization
- Analytics and feedback collection

## Current launch limitations

- Render Free Web Services can sleep when inactive.
- Render Free PostgreSQL is temporary and expires after 30 days; export data before expiration.
- Local uploads are not durable across service replacement or redeployment.
- The seeded demo data must be cleaned before public sharing.
- Google sign-in should remain disabled until its allowed origins are configured.
