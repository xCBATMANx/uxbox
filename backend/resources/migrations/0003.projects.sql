-- Tables

CREATE TABLE IF NOT EXISTS projects (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  modified_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  deleted_at timestamptz DEFAULT NULL,

  name text NOT NULL,
  metadata bytea NULL DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS project_users (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  project_id uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,

  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  modified_at timestamptz NOT NULL DEFAULT clock_timestamp(),

  can_edit boolean DEFAULT false,

  PRIMARY KEY (user_id, project_id)
);

CREATE TABLE IF NOT EXISTS project_files (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  project_id uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,

  name text NOT NULL,

  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  modified_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  deleted_at timestamptz DEFAULT NULL,

  metadata bytea NULL DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS project_file_users (
  file_id uuid NOT NULL REFERENCES project_files(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  modified_at timestamptz NOT NULL DEFAULT clock_timestamp(),

  can_edit boolean DEFAULT false,

  PRIMARY KEY (user_id, file_id)
);

CREATE TABLE IF NOT EXISTS project_pages (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),

  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  file_id uuid NOT NULL REFERENCES project_files(id) ON DELETE CASCADE,

  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  modified_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  deleted_at timestamptz DEFAULT NULL,

  version bigint NOT NULL,
  ordering smallint NOT NULL,

  name text NOT NULL,
  data bytea NOT NULL,
  metadata bytea NULL DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS project_page_history (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),

  user_id uuid NULL REFERENCES users(id) ON DELETE SET NULL,
  page_id uuid NOT NULL REFERENCES project_pages(id) ON DELETE CASCADE,

  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  modified_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  version bigint NOT NULL DEFAULT 0,

  pinned bool NOT NULL DEFAULT false,
  label text NOT NULL DEFAULT '',

  data bytea NOT NULL
);

-- Indexes

CREATE INDEX projects__user_id__idx ON projects(user_id);

CREATE INDEX project_files__user_id__idx ON project_files(user_id);
CREATE INDEX project_files__project_id__idx ON project_files(project_id);

CREATE INDEX project_pages__user_id__idx ON project_pages(user_id);
CREATE INDEX project_pages__file_id__idx ON project_pages(file_id);

CREATE INDEX project_page_history__page_id__idx ON project_page_history(page_id);
CREATE INDEX project_page_history__user_id__idx ON project_page_history(user_id);

-- Triggers

CREATE OR REPLACE FUNCTION handle_project_insert()
  RETURNS TRIGGER AS $$
  BEGIN
    INSERT INTO project_users (user_id, project_id, can_edit)
    VALUES (NEW.user_id, NEW.id, true);

    RETURN NEW;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION handle_page_update()
  RETURNS TRIGGER AS $pagechange$
  DECLARE
    current_dt timestamptz := clock_timestamp();
  BEGIN
    --- Update projects modified_at attribute when a
    --- page of that project is modified.
    UPDATE projects SET modified_at = current_dt
     WHERE id = OLD.project_id;

    UPDATE files SET modified_at = current_dt
     WHERE id = OLD.project_id;

    RETURN NEW;
  END;
$pagechange$ LANGUAGE plpgsql;

CREATE TRIGGER projects_on_insert_tgr
 AFTER INSERT ON projects
   FOR EACH ROW EXECUTE PROCEDURE handle_project_insert();

CREATE TRIGGER pages__on_update__tgr
BEFORE UPDATE ON project_pages
   FOR EACH ROW EXECUTE PROCEDURE handle_page_update();


CREATE TRIGGER projects__modified_at__tgr
BEFORE UPDATE ON projects
   FOR EACH ROW EXECUTE PROCEDURE update_modified_at();

CREATE TRIGGER project_files__modified_at__tgr
BEFORE UPDATE ON project_files
   FOR EACH ROW EXECUTE PROCEDURE update_modified_at();

CREATE TRIGGER project_pages__modified_at__tgr
BEFORE UPDATE ON project_pages
   FOR EACH ROW EXECUTE PROCEDURE update_modified_at();

CREATE TRIGGER project_page_history__modified_at__tgr
BEFORE UPDATE ON project_page_history
   FOR EACH ROW EXECUTE PROCEDURE update_modified_at();
