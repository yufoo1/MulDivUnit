`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: 
// 
// Create Date:    18:17:21 11/14/2022 
// Design Name: 
// Module Name:    MulDivUnit 
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
module MulDivUnit(
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
	 wire[31:0] mul_out_res[1:0];
	 wire[31:0] div_out_res[1:0];
	 reg[1:0] op;
	 always@(posedge clock) begin
		if(reset) begin
			op  <= 'h0;
		end else if(in_ready & in_valid) begin
			op <= in_op;
		end else if(out_ready & out_valid) begin
			op <= 'h0;
		end
	 end
	 MulUnit MulUnit(
			.clock(clock), 
			.reset(reset), 
			.in_src0(in_src0),
			.in_src1(in_src1),
			.in_op(in_op), 
			.in_sign(in_sign), 
			.in_ready(mul_in_ready), 
			.in_valid(in_valid), 
			.out_ready(out_ready), 
			.out_valid(mul_out_valid), 
			.out_res0(mul_out_res[0]),
			.out_res1(mul_out_res[1]));
			
	 DivUnit DivUnit(
			.clock(clock), 
			.reset(reset), 
			.in_src0(in_src0),
			.in_src1(in_src1),
			.in_op(in_op), 
			.in_sign(in_sign), 
			.in_ready(div_in_ready), 
			.in_valid(in_valid), 
			.out_ready(out_ready), 
			.out_valid(div_out_valid), 
			.out_res0(div_out_res[0]),
			.out_res1(div_out_res[1]));
	 assign in_ready = mul_in_ready & div_in_ready;
	 assign out_valid = mul_out_valid | div_out_valid;
	 assign {out_res1, out_res0} = (in_op == 'd2) ? {div_out_res[1], div_out_res[0]} : {mul_out_res[1], mul_out_res[0]};
endmodule
