# Teammate guide: how to do an assigned task

One issue = one branch = one pull request. Never push straight to `main`.

## Once
1. Accept the invitation: https://github.com/miladmalikzada/smart-dormitory-services/invitations
2. Clone and set your identity (use the e-mail of your GitHub account so commits show under your profile):
   ```bash
   git clone https://github.com/miladmalikzada/smart-dormitory-services
   cd smart-dormitory-services
   git config user.name "Your Name"
   git config user.email "you@example.com"
   python3 scripts/setup_env.py     # creates your private .env, never commit it
   ```

## For each task
```bash
git switch main && git pull
git switch -c feature/7-visitor-log-tests      # issue number + short name
# work in small steps and commit each one:
git add -A
git commit -m "Add visitor log tests (#7)"
mvn verify                                      # must be green
git push -u origin feature/7-visitor-log-tests
```
Then open the pull request on GitHub. Write `Closes #7`, name the design pattern involved, and ask the other teammate to review.

## Review rules
- Read the **Files changed** tab. Ask at least one real question about the design.
- Choose **Approve** only when CI is green and your question is answered.
- The author answers or changes the code, then merges.

## Do not
- commit `.env` or any real personal data (use made-up IIN and passport numbers only);
- force-push, or push to `main`;
- merge your own pull request without a review.

## Who does what
| Issue | Task |
|---|---|
| #6, #8, #9, #11 | Iqbal Habibi |
| #7, #10, #12 | habibiahmadbilal |
