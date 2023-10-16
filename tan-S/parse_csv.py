import csv
import os
import sys

if len(sys.argv) < 2:
    print('Usage: python3 parse_csv.py <csv file>')
    sys.exit(1)

csv_file = sys.argv[1]
output_dir = os.path.splitext(os.path.basename(csv_file))[0]

with open(csv_file, 'r') as f:
    reader = csv.DictReader(f)
    data = list(reader)
    for record in data:
        if not os.path.exists(os.path.join(output_dir, record['Category'])):
            os.makedirs(os.path.join(output_dir, record['Category']))
        open(os.path.join(output_dir, record['Category'], record['Test'][:-4] + '.txt'), 'w').write(record['Expected'])
