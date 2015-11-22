DROP TABLE stats_remote;
CREATE TABLE stats_remote (
  node_id            BIGINT NOT NULL,
  origin             VARCHAR(64),
  download_count     BIGINT,
  last_downloaded    BIGINT,
  last_downloaded_by VARCHAR(64),
  path VARCHAR(1024),
  CONSTRAINT stats_remote_pk PRIMARY KEY (node_id, origin),
  CONSTRAINT stats_remote_nodes_fk FOREIGN KEY (node_id) REFERENCES nodes (node_id)
);
