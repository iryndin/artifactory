DROP TABLE stats_remote;
CREATE TABLE stats_remote (
  node_id            NUMBER(19, 0) NOT NULL,
  origin             VARCHAR2(64),
  download_count     NUMBER(19, 0),
  last_downloaded    NUMBER(19, 0),
  last_downloaded_by VARCHAR2(64),
  path VARCHAR(1024),
  CONSTRAINT stats_remote_pk PRIMARY KEY (node_id, origin),
  CONSTRAINT stats_remote_nodes_fk FOREIGN KEY (node_id) REFERENCES nodes (node_id)
);
