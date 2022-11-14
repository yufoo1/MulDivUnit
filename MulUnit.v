`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: 
// 
// Create Date:    20:37:23 11/14/2022 
// Design Name: 
// Module Name:    MulUnit 
// Project Name: 
// Target Devices: 
// Tool versions: 
// Description: 
//
// Dependencies: 
//
// Revision: 
// Revision 0.01 - File Created
// Additional Comments: 
//
//////////////////////////////////////////////////////////////////////////////////
module MulUnit(
	 input clock,
	 input reset,
    input [31:0] in_src0,
	 input [31:0] in_src1,
    input [1:0] in_op,
    input in_sign,
    output in_ready,
    input in_valid,
    input out_ready,
    output out_valid,
    output [31:0] out_res0,
	 output [31:0] out_res1
    );
	 reg done;
	 reg[63:0] tmp;
	 always@(posedge clock) begin
		if(reset) begin
			done <= 'h0;
			tmp <= 'h0;
		end else if(in_valid & in_ready & (in_op == 'd1)) begin
			tmp <= in_sign ? $signed(in_src0) * $signed(in_src1) : in_src0 * in_src1;
			done <= 'h1;
		end else if(out_valid & out_ready) begin
			tmp <= 'h0;
			done <= 'h0;
		end
	 end
	 assign {out_res1, out_res0, in_ready, out_valid} = {tmp, !done, done};

endmodule
