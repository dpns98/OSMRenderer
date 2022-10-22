import xml.sax, sqlite3, sys, math, tripy

R =  6378137.0
def lat2y(lat):
    return math.log(math.tan(math.pi / 4 + math.radians(lat) / 2)) * R
def lon2x(lon):
    return math.radians(lon) * R

db_connect = None     # SQLite Database connection
db         = None     # SQLite Database cursor

class OsmHandler(xml.sax.ContentHandler):
    def __init__(self):
        self.element_way_active = False
        self.way_id = -1

    def startElement(self, element, attrib):
        if element == 'node':
            db.execute('INSERT INTO nodes (node_id,lon,lat) VALUES (?,?,?)',
             (attrib['id'], lon2x(float(attrib['lon'])), lat2y(float(attrib['lat']))))
        elif element == 'tag' and self.element_way_active and attrib['k'] in ('building', 'highway', 'landuse', 'natural', 'boundary'):
            db.execute('INSERT INTO way_tags (way_id,key,value) VALUES (?,?,?)',
             (self.way_id, attrib['k'], attrib['v']))
        elif element == 'way':
            self.element_way_active = True
            self.way_id = attrib['id']
        elif element == 'nd':
            db.execute('INSERT INTO way_nodes (way_id,node_id) VALUES (?,?)',
             (self.way_id, attrib['ref']))

    def endElement(self, element):
        if element == 'way':
            self.element_way_active = False
            self.way_id = -1

def add_tables():
    db.execute('''
    CREATE TABLE nodes (
     node_id      INTEGER PRIMARY KEY,  -- node ID
     lon          REAL,                 -- longitude
     lat          REAL                  -- latitude
    )
    ''')
    db.execute('''
    CREATE TABLE way_nodes (
     way_id       INTEGER,              -- way ID
     node_id      INTEGER               -- node ID
    )
    ''')
    db.execute('''
    CREATE TABLE way_tags (
     way_id       INTEGER,              -- way ID
     key          TEXT,                 -- tag key
     value        TEXT                  -- tag value
    )
    ''')

def add_std_index():
    db.execute('CREATE INDEX way_tags__way_id              ON way_tags (way_id)')
    db.execute('CREATE INDEX way_tags__key                 ON way_tags (key)')
    db.execute('CREATE INDEX way_nodes__way_id             ON way_nodes (way_id)')
    db.execute('CREATE INDEX way_nodes__node_id            ON way_nodes (node_id)')
    db_connect.commit()

def add_rtree_ways():
    db.execute('CREATE VIRTUAL TABLE rtree_way1 USING rtree(way_id, min_lat, max_lat, min_lon, max_lon)')
    db.execute('''
    INSERT INTO rtree_way1 (way_id, min_lat, max_lat, min_lon, max_lon)
    SELECT way_nodes.way_id,min(nodes.lat),max(nodes.lat),min(nodes.lon),max(nodes.lon)
    FROM way_nodes
    LEFT JOIN nodes ON way_nodes.node_id=nodes.node_id
    JOIN way_tags ON way_tags.way_id=way_nodes.way_id
	WHERE way_tags.key IN ('building', 'highway', 'landuse', 'natural', 'boundary')
    GROUP BY way_nodes.way_id
    ''')
    db.execute('CREATE VIRTUAL TABLE rtree_way2 USING rtree(way_id, min_lat, max_lat, min_lon, max_lon)')
    db.execute('''
    INSERT INTO rtree_way2 (way_id, min_lat, max_lat, min_lon, max_lon)
    SELECT way_nodes.way_id,min(nodes.lat),max(nodes.lat),min(nodes.lon),max(nodes.lon)
    FROM way_nodes
    LEFT JOIN nodes ON way_nodes.node_id=nodes.node_id
	JOIN way_tags ON way_tags.way_id=way_nodes.way_id
	WHERE way_tags.key IN ('highway', 'landuse', 'natural', 'boundary')
    GROUP BY way_nodes.way_id
    ''')
    db_connect.commit()

def main():
    global db_connect, db
    db_connect = sqlite3.connect(sys.argv[2])
    db = db_connect.cursor()
    db.execute('PRAGMA journal_mode = OFF');
    db.execute('PRAGMA page_size = 65536');
    add_tables()
    parser = xml.sax.make_parser()
    parser.setFeature(xml.sax.handler.feature_namespaces, 0)
    handler = OsmHandler()
    parser.setContentHandler(handler)
    parser.parse(sys.argv[1])
    db_connect.commit()
    add_std_index()
    add_rtree_ways()
    print("done")

if __name__ == "__main__":
    main()

