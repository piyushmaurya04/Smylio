# GitHub Actions CI/CD

## Architecture

The `CI/CD` workflow runs on GitHub-hosted Ubuntu runners. It validates pull
requests targeting `release/Smylio`, pushes to `release/Smylio`, and pushes to
`feature/**` branches. It does not contain a deployment command or a deploy
hook.

For production, Render's native **After CI Checks Pass** auto-deploy option
monitors checks for commits on the Render service's connected branch. Configure
that service to track `release/Smylio`. Render then starts its own deployment
only after the commit's checks pass. Pull requests and feature-branch pushes
run CI but are not the branch linked to the production service.

The workflow's single required job is named **CI checks** (check context:
`CI checks`; GitHub may qualify it with the workflow name in branch-rule
settings). Its steps run in this order:

1. Check out the repository.
2. Set up Java 17 and Node.js 22, with Maven and npm dependency caches.
3. Run `npm ci`, `npm run lint`, and `npm run build` in `frontend/`.
4. Run `./mvnw -B verify`.
5. Upload Surefire/Failsafe JUnit XML reports, including when verification
   fails.
6. Build the root `Dockerfile` as a validation step; the image is not published.

The Maven step uses a disposable PostgreSQL 16 service provided by the GitHub
runner and `spring.jpa.hibernate.ddl-auto=create-drop`. Tests therefore create
and discard their schema in that CI database; they do not connect to or modify
the production Supabase database. The database values and JWT key in the
workflow are test-only values, not repository or Render credentials.

## Configure Render's CI gate

The Render dashboard must be configured manually; repository changes cannot
change the existing service's settings:

1. Before merging this workflow into `release/Smylio`, open the existing
   production **Web Service** in the Render Dashboard. If **Auto-Deploy** is
   **On Commit**, change it to **After CI Checks Pass**. Verify the setting
   before merging; **On Commit** could deploy the merge before CI gates it.
2. Confirm its connected repository is `piyushmaurya04/Smylio` and its branch is
   `release/Smylio`.
3. Save the setting and confirm **After CI Checks Pass** is active.
4. Confirm the service is configured to build from the repository's root
   `Dockerfile`, with health check path `/actuator/health`.
5. Keep `SUPABASE_JDBC_URL`, `SUPABASE_DB_USERNAME`, `SUPABASE_DB_PASSWORD`,
   and `JWT_SECRET` in the Render service's environment settings only.

Render's current [deploy documentation](https://render.com/docs/deploys#integrating-with-ci)
says this setting waits for the new commit's CI checks; GitHub Actions checks
are supported. Render considers conclusions `success`, `neutral`, and
`skipped` successful. It does not deploy if it detects zero checks or at least
one failed check. A cancelled check is not among the accepted conclusions and
does not satisfy the gate. The workflow deliberately avoids conditional job
skips so the CI check is present for every push to the production branch.

Render evaluates checks associated with the commit, not only the check marked
required by GitHub branch protection. Keep the production workflow's check
visible on each commit, and avoid configuration that suppresses it or reports
it as skipped.

This native integration does not make Render's deployment-completion status
part of the GitHub Actions run. There is no deploy-hook workflow here, so there
is no duplicate deployment path or deploy-hook secret.

The workflow concurrency group cancels older in-progress CI runs for the same
branch or pull request. GitHub concurrency cannot cancel a Render deployment
that has already started; confirm the deployed commit SHA in Render's Deploys
page when multiple production commits arrive close together.

## GitHub branch protection

In the repository's **Settings → Branches** (or rulesets), add a rule for
`release/Smylio`:

- Require a pull request before merging.
- Require status checks to pass before merging; after the first workflow run,
  select the job check named **CI checks** (GitHub may display it as
  **CI/CD / CI checks**).
- Require the branch to be up to date before merging if that fits the team's
  merge policy.
- Restrict direct pushes or force pushes as appropriate for the team.

Branch protection prevents merging a failing change. Render's **After CI
Checks Pass** setting is the separate deployment gate; enabling branch
protection alone does not change Render's auto-deploy behavior.

## Secrets and permissions

The workflow needs no repository secrets and has only `contents: read`
workflow-token permission. Pull request runs do not receive production
credentials. Do not add Supabase credentials, `JWT_SECRET`, a Render API key,
or a deploy-hook URL to GitHub Actions secrets for this workflow.

No `RENDER_DEPLOY_HOOK_URL` or production URL secret is required. The production
URL was not provided, and native Render auto-deploy does not report deployment
completion to this workflow. Keep production configuration in Render.

## Safely test the workflow

1. Create a temporary branch matching `feature/**` and push a harmless change.
   Confirm the frontend, backend, report-upload, and Docker-build steps run.
   The production Render service tracks `release/Smylio`, so this feature push
   must not deploy.
2. To test a failure, make a temporary change on that feature branch that
   deliberately causes one CI command or test to fail. Confirm the `CI checks`
   job fails and reports are uploaded when available. Do not merge that change;
   remove or abandon the temporary branch afterward.
3. For a pull-request run, open a draft PR from the temporary branch targeting
   `release/Smylio`. Confirm it runs checks only. Do not merge it as part of
   this test.
4. To exercise the production deployment gate, first enable **After CI Checks
   Pass** in Render, then use a reviewed, intended change to `release/Smylio`.
   Do not test the production gate by pushing deliberately failing code to the
   production branch.

## Verify a production deployment

After CI succeeds, check the Render service's **Events** or **Deploys** page.
Wait for the deployment for the expected commit SHA to finish and show **Live**;
a successful GitHub workflow only proves the checks passed, not that production
is healthy.

The workflow cannot automatically wait for Render's native deployment or
verify it. Once Render reports the deployment as **Live**, set the service URL
in your local shell and request the configured health endpoint. For example,
in PowerShell:

```powershell
$env:PRODUCTION_URL = "https://<your-render-service>"
$healthUrl = "$($env:PRODUCTION_URL.TrimEnd('/'))/actuator/health"
$response = Invoke-WebRequest -Uri $healthUrl -Method Get
$response.Content
```

Confirm the response is HTTP 200 and reports `"status":"UP"`. The endpoint
checks database connectivity and does not expose health details. Do not put
credentials in the URL or command.

## Troubleshooting and rollback

- **No deployment starts:** confirm Render tracks `release/Smylio`, the setting
  is **After CI Checks Pass**, and GitHub Actions has posted a check for the
  exact commit. Render intentionally does not deploy when it detects no checks.
- **A check fails or is cancelled:** inspect the `CI checks` log and the
  `maven-test-reports` artifact. Resolve the failure and push a new commit;
  do not manually deploy the failed commit to bypass the gate.
- **Maven database errors:** verify the workflow's disposable PostgreSQL
  service starts and the step uses its CI-only connection values. Never replace
  them with production Supabase credentials.
- **Health check fails after Render says Live:** inspect Render deployment and
  runtime logs, verify the Render environment variables and health-check path,
  and investigate Supabase connectivity.
- **Rollback:** use Render's Deploys page to redeploy a known-good deployment
  if needed, then fix forward with a reviewed commit through the normal CI gate.
  A manual rollback is an operational action and does not make a failed CI run
  pass.
