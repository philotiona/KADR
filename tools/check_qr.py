"""Compare the production Java QR matrix with ReportLab's independent encoder."""
from pathlib import Path
from reportlab.graphics.barcode import qrencoder as q

reference = q.QRCode(1, q.QRErrorCorrectLevel.L)
reference.addData(q.QR8bitByte("KDR-1234ABCD"))
reference.makeImpl(False, 0)
actual = Path("build/cinema-tests/qr-matrix.txt").read_text().splitlines()
expected = ["".join("1" if cell else "0" for cell in row) for row in reference.modules]
assert actual == expected, "QR matrix differs from independent encoder"
print("PASS: all 441 QR modules match ReportLab")
