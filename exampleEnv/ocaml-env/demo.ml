let rec shift lst n =
  let addToEnd lst = match lst with
    | [] -> []
    | h::tl -> tl @ [h]

  in if n > 0 then
    shift (addToEnd lst) (n-1)
  else
    lst;;

let print_lst printer lst =
  let rec print_lst_helper lst =
    match lst with
    | [] -> ()
    | h::tl -> printer h; print_string " "; print_lst_helper tl 
  in
  print_lst_helper lst; print_string "\n";;

let print_int_lst = print_lst print_int;;
let lst = [1; 2; 3; 4];; 
  
print_int_lst (shift lst 0);;
print_int_lst (shift lst 1);;
print_int_lst (shift lst 2);;
print_int_lst (shift lst 3);;
print_int_lst (shift lst 4);;