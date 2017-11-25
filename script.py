enums = []
while True:
    try:
        line = raw_input()
        parts = line.split(':')
        
        spec = 'SPEC_' + parts[0];
        enums += [inst + '(' + spec + ')' for inst in  parts[1].split(',')]
    except:
        break

print(', '.join(sorted(enums)))


