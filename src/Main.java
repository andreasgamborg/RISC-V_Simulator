public class Main {
    static int pc;
    static int reg[] = new int[4];

    // Here the first program hard coded as an array
    static int progr[] = {
            // As minimal RISC-V assembler example
            0x00200093, // addi x1 x0 2
            0x00300113, // addi x2 x0 3
            0x00a00113, // addi x2 x0 10
            0x00a00113, // addi x2 x0 10
            0x002081b3, // add x3 x1 x2
    };

    public static void main(String[] args) {

        System.out.println("Hello RISC-V World!");
        pc = 0;

        for (;;) {

            int instr = progr[pc];
            int opcode = instr & 0x7f;
            int rd = (instr >> 7) & 0x01f;
            int rs1 = (instr >> 15) & 0x01f;
            int imm = (instr >> 20);

            switch (opcode) {

                case 0x13:
                    reg[rd] = reg[rs1] + imm;
                    break;
                default:
                    System.out.println("Opcode " + opcode + " not yet implemented");
                    break;
            }

            ++pc; // We count in 4 byte words
            if (pc >= progr.length) {
                break;
            }
            for (int i = 0; i < reg.length; ++i) {
                System.out.print(reg[i] + " ");
            }
            System.out.println();
        }
        

        System.out.println("Program exit hej nej");
        

    }

}