# GitLab Notificator

A Spring Boot application that sends real-time notifications from GitLab to Telegram, helping developers and team leads stay updated about project activities.

## Features

- **Real-time notifications** for various GitLab events:
    - Merge Request creation, approval, merging
    - Comments on Merge Requests
    - New issues creation
    - Tag creation
    - Pipeline status changes (success/failure)
    - Deployment notifications

- **Role-based notifications**:
    - Developers receive notifications about their assigned MRs and reviews
    - Team leads receive all important project notifications

- **Telegram integration**:
    - Markdown formatted messages with emojis
    - Inline buttons for quick access to GitLab objects
    - Customizable notification settings

## How It Works

1. The application receives webhook events from GitLab
2. Event handlers process different types of events (Merge Requests, Issues, Pipelines, etc.)
3. Notifications are formatted with relevant information and Telegram keyboard buttons
4. Messages are sent to appropriate Telegram users based on their role and GitLab user mapping

## Setup and Installation

### Prerequisites

- Java 17 or higher
- Telegram Bot Token (from [@BotFather](https://t.me/BotFather))
- GitLab instance with webhook permissions
- (Optional) PostgreSQL database for production

### Configuration

1. Clone the repository
2. Configure environment variables:

```bash
# Telegram configuration
export TG_BOT_TOKEN=your_telegram_bot_token
export TG_BOT_USERNAME=your_bot_username

# GitLab configuration
export GITLAB_WEBHOOK_SECRET=your_webhook_secret
```

3. Configure database (choose one):

**For development (H2 database):**
- Uses embedded H2 database (no additional setup required)
- Access console at `/h2-console`

**For production (PostgreSQL):**
- Configure environment variables

```bash
export SPRING_PROFILES_ACTIVE=postgre
# Postgres configuration
export SPRING_DATASOURCE_URL=your_postgre_jdbc_url
export SPRING_DATASOURCE_USERNAME=your_postgre_username
export SPRING_DATASOURCE_PASSWORD=your_postgre_password
```

### Running the Application

**Using Docker:**
```bash
docker build -t gitlab-notificator .
docker run -p 8080:8080 \
  -e TG_BOT_TOKEN=your_token \
  -e GITLAB_WEBHOOK_SECRET=your_secret \
  gitlab-notificator
```

Add environment variables for Postgres configuration if needed

**Using Gradle:**
```bash
./gradlew bootRun
```

**As a JAR:**
```bash
./gradlew bootJar
java -jar build/libs/*SNAPSHOT.jar
```

### GitLab Webhook Configuration

In your GitLab project settings:

1. Go to Webhooks section
2. Set URL: `https://your-domain.com/webhook/gitlab`
3. Set Secret Token: matches your `GITLAB_WEBHOOK_SECRET`
4. Select events to receive:
    - Merge Request events
    - Note events (comments)
    - Issue events
    - Tag push events
    - Pipeline events

### User Mapping

Map GitLab users to Telegram IDs in the database:

1. The application uses H2/PostgreSQL to store user mappings
2. Users need to be manually mapped in the `user_mapping` table
3. Each mapping connects a GitLab user ID to a Telegram chat ID

## Project Structure

- `handler/` - Event handlers for different GitLab event types
- `model/` - Data transfer objects and entity classes
- `service/` - Business logic and Telegram integration
- `controller/` - Webhook endpoint controller
- `util/` - Utility classes for message formatting

## Supported Events

- Merge Request: create, approve, merge, undraft, assign reviewer
- Comments on Merge Requests
- New Issue creation
- New Tag creation
- Pipeline status changes (success/failure)
- Deployment notifications