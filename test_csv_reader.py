import csv

csv_file_path = '/Users/harshsharma/Hivemynds/hcc-service/icd10code_data.csv'

def test_reader():
    with open(csv_file_path, newline='', encoding='utf-8-sig') as csvfile:
        reader = csv.reader(csvfile)
        for i in range(10):
            try:
                row = next(reader)
                print(f"Row {i}: {row}")
            except StopIteration:
                break

if __name__ == "__main__":
    test_reader()
